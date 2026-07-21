package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ProgressService {

  private final ProgressRepository progressRepo;
  private final DocRepository docRepo;
  private final IslandRepository islandRepo;

  public ProgressService(ProgressRepository progressRepo, DocRepository docRepo, IslandRepository islandRepo) {
    this.progressRepo = progressRepo;
    this.docRepo = docRepo;
    this.islandRepo = islandRepo;
  }

  @Transactional
  public ProgressItemDto upsert(Long userId, Long docId, ProgressStatus status, Integer progressPct) {
    int pct = progressPct == null ? 0 : Math.max(0, Math.min(100, progressPct));
    if (status == ProgressStatus.COMPLETED) pct = 100;

    // 单调不回退、状态只升不降、completedAt 首完写入等合并语义由
    // ProgressRepository.upsertAtomic 的 INSERT ... ON CONFLICT 在数据库侧原子完成,
    // 并发首写不再撞唯一约束;此处只负责把入参规整为「本次上报值」。
    Instant now = Instant.now();
    Instant lastReadAt = (pct > 0 || status != ProgressStatus.NOT_STARTED) ? now : null;
    Instant completedAt = (status == ProgressStatus.COMPLETED) ? now : null;
    progressRepo.upsertAtomic(userId, docId, status.name(), pct, lastReadAt, completedAt);

    Progress p = progressRepo.findById(new ProgressId(userId, docId))
        .orElseThrow(() -> new IllegalStateException("进度写入后读取失败"));
    // 小岛解锁状态由 getAggregate 实时计算（顺序解锁），无需在此持久化。
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

    Map<Long, Progress> progByDoc = progresses.stream()
        .collect(Collectors.toMap(Progress::getDocId, pp -> pp));
    Map<Long, List<Doc>> docsByIsland = docRepo.findAll().stream()
        .filter(Doc::getActive)
        .collect(Collectors.groupingBy(Doc::getIslandId));

    Map<Long, IslandStatus> statusById = computeAllIslandStatuses(docsByIsland, progByDoc);

    List<IslandStateViewDto> islands = new ArrayList<>();
    for (Island island : islandRepo.findAllByOrderByOrderAsc()) {
      List<Doc> islandDocs = docsByIsland.getOrDefault(island.getId(), List.of());
      long completed = islandDocs.stream()
          .filter(d -> {
            Progress pp = progByDoc.get(d.getId());
            return pp != null && pp.getStatus() == ProgressStatus.COMPLETED;
          })
          .count();
      islands.add(new IslandStateViewDto(island.getId(),
          statusById.getOrDefault(island.getId(), IslandStatus.LOCKED),
          (int) completed, islandDocs.size()));
    }
    return new ProgressAggregateDto(docs, islands);
  }

  /**
   * 顺序解锁：每个机构内按 order 排序，首个小岛默认 UNLOCKED（入口），
   * 后续小岛仅在前一小岛 COMPLETED 时解锁。已完成本岛全部必修则 COMPLETED。
   * 支持直链进入：本岛已有阅读记录则保持 UNLOCKED。
   */
  private Map<Long, IslandStatus> computeAllIslandStatuses(Map<Long, List<Doc>> docsByIsland,
                                                            Map<Long, Progress> progByDoc) {
    // findAllByOrderByOrderAsc 全局按 order 排序；按机构分组后，组内仍是 order 升序。
    Map<Long, List<Island>> byInstitution = islandRepo.findAllByOrderByOrderAsc().stream()
        .collect(Collectors.groupingBy(Island::getInstitutionId));
    Map<Long, IslandStatus> result = new HashMap<>();
    for (List<Island> group : byInstitution.values()) {
      boolean prevCompleted = true; // 机构首个小岛为入口
      for (Island island : group) {
        List<Doc> islandDocs = docsByIsland.getOrDefault(island.getId(), List.of());
        IslandStatus status = computeIslandStatus(islandDocs, progByDoc, prevCompleted);
        result.put(island.getId(), status);
        prevCompleted = (status == IslandStatus.COMPLETED);
      }
    }
    return result;
  }

  /** 规则：必修全完成→COMPLETED；否则(前岛已完成 或 本岛已开始)→UNLOCKED；否则 LOCKED。 */
  private IslandStatus computeIslandStatus(List<Doc> islandDocs, Map<Long, Progress> progByDoc,
                                           boolean prevIslandCompleted) {
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
    if (prevIslandCompleted || anyStarted) return IslandStatus.UNLOCKED;
    return IslandStatus.LOCKED;
  }
}
