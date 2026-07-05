package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ProgressService {

  private final ProgressRepository progressRepo;
  private final IslandStateRepository islandStateRepo;
  private final DocRepository docRepo;
  private final IslandRepository islandRepo;

  public ProgressService(ProgressRepository progressRepo, IslandStateRepository islandStateRepo,
                         DocRepository docRepo, IslandRepository islandRepo) {
    this.progressRepo = progressRepo;
    this.islandStateRepo = islandStateRepo;
    this.docRepo = docRepo;
    this.islandRepo = islandRepo;
  }

  @Transactional
  public ProgressItemDto upsert(Long userId, Long docId, ProgressStatus status, Integer progressPct) {
    int pct = progressPct == null ? 0 : Math.max(0, Math.min(100, progressPct));
    if (status == ProgressStatus.COMPLETED) pct = 100;

    Progress p = progressRepo.findById(new ProgressId(userId, docId)).orElseGet(() -> {
      Progress np = new Progress();
      np.setUserId(userId);
      np.setDocId(docId);
      np.setStatus(ProgressStatus.NOT_STARTED);
      np.setProgressPct(0);
      return np;
    });

    // 单调不回退：只增不减
    if (pct < p.getProgressPct()) pct = p.getProgressPct();
    p.setProgressPct(pct);
    if (pct > 0 || status != ProgressStatus.NOT_STARTED) {
      p.setLastReadAt(Instant.now());
    }
    if (status.ordinal() > p.getStatus().ordinal()) {
      p.setStatus(status);
    }
    if (p.getStatus() == ProgressStatus.COMPLETED) {
      p.setProgressPct(100);
      if (p.getCompletedAt() == null) p.setCompletedAt(Instant.now());
    }
    progressRepo.save(p);

    recomputeIslandState(userId, p.getDocId());
    return ProgressItemDto.from(p);
  }

  @Transactional
  public ProgressItemDto complete(Long userId, Long docId) {
    return upsert(userId, docId, ProgressStatus.COMPLETED, 100);
  }

  @Transactional(readOnly = true)
  public ProgressAggregateDto getAggregate(Long userId) {
    List<Progress> progresses = progressRepo.findByUserId(userId);
    List<ProgressItemDto> docs = progresses.stream().map(ProgressItemDto::from).toList();

    // 聚合小岛状态
    Map<Long, List<Doc>> docsByIsland = docRepo.findAll().stream()
        .filter(Doc::getActive)
        .collect(Collectors.groupingBy(Doc::getIslandId));
    Map<Long, Progress> progByDoc = progresses.stream()
        .collect(Collectors.toMap(Progress::getDocId, pp -> pp));

    List<IslandStateViewDto> islands = new ArrayList<>();
    for (Island island : islandRepo.findAllByOrderByOrderAsc()) {
      List<Doc> islandDocs = docsByIsland.getOrDefault(island.getId(), List.of());
      long completed = islandDocs.stream()
          .filter(d -> {
            Progress pp = progByDoc.get(d.getId());
            return pp != null && pp.getStatus() == ProgressStatus.COMPLETED;
          })
          .count();
      int total = islandDocs.size();
      IslandStatus status = computeIslandStatus(islandDocs, progByDoc);
      islands.add(new IslandStateViewDto(island.getId(), status, (int) completed, total));
    }
    return new ProgressAggregateDto(docs, islands);
  }

  private void recomputeIslandState(Long userId, Long docId) {
    Doc doc = docRepo.findById(docId).orElse(null);
    if (doc == null) return;
    Long islandId = doc.getIslandId();
    List<Doc> islandDocs = docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(islandId);
    Map<Long, Progress> progByDoc = progressRepo.findByUserId(userId).stream()
        .collect(Collectors.toMap(Progress::getDocId, p -> p));
    IslandStatus status = computeIslandStatus(islandDocs, progByDoc);

    IslandState st = islandStateRepo.findById(new IslandStateId(userId, islandId))
        .orElseGet(() -> {
          IslandState ns = new IslandState();
          ns.setUserId(userId);
          ns.setIslandId(islandId);
          ns.setStatus(IslandStatus.LOCKED);
          return ns;
        });
    st.setStatus(status);
    islandStateRepo.save(st);
  }

  /** 规则：必修文档全完成 → COMPLETED；至少有一个非 NOT_STARTED → UNLOCKED；否则 LOCKED。 */
  private IslandStatus computeIslandStatus(List<Doc> islandDocs, Map<Long, Progress> progByDoc) {
    List<Doc> required = islandDocs.stream().filter(Doc::getRequired).toList();
    boolean allRequiredDone = !required.isEmpty() && required.stream().allMatch(d -> {
      Progress p = progByDoc.get(d.getId());
      return p != null && p.getStatus() == ProgressStatus.COMPLETED;
    });
    if (allRequiredDone) return IslandStatus.COMPLETED;
    boolean anyStarted = islandDocs.stream().anyMatch(d -> {
      Progress p = progByDoc.get(d.getId());
      return p != null && p.getStatus() != ProgressStatus.NOT_STARTED;
    });
    return anyStarted ? IslandStatus.UNLOCKED : IslandStatus.LOCKED;
  }
}
