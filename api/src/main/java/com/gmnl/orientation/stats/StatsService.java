package com.gmnl.orientation.stats;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Institution;
import com.gmnl.orientation.content.InstitutionRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import com.gmnl.orientation.progress.Progress;
import com.gmnl.orientation.progress.ProgressRepository;
import com.gmnl.orientation.progress.ProgressStatus;
import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import com.gmnl.orientation.user.UserRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 全员学习统计：一次加载 users/docs/progress，在内存批量计算，避免 N+1。
 * 完成规则与 ProgressService 一致——小岛全部必修文档完成即视为该小岛完成。
 */
@Service
public class StatsService {

  private final UserRepository userRepo;
  private final DocRepository docRepo;
  private final IslandRepository islandRepo;
  private final InstitutionRepository institutionRepo;
  private final ProgressRepository progressRepo;

  public StatsService(UserRepository userRepo, DocRepository docRepo, IslandRepository islandRepo,
                      InstitutionRepository institutionRepo, ProgressRepository progressRepo) {
    this.userRepo = userRepo;
    this.docRepo = docRepo;
    this.islandRepo = islandRepo;
    this.institutionRepo = institutionRepo;
    this.progressRepo = progressRepo;
  }

  @Transactional(readOnly = true)
  public List<UserStatsDto> userStats() {
    return compute().userStats;
  }

  @Transactional(readOnly = true)
  public StatsOverviewDto overview() {
    return compute().overview;
  }

  private Snapshot compute() {
    List<User> users = userRepo.findByRole(UserRole.USER);
    List<Doc> docs = docRepo.findAll();
    List<Island> islands = islandRepo.findAllByOrderByOrderAsc();
    List<Institution> institutions = institutionRepo.findAllByOrderByOrderAsc();
    Map<Long, String> instName = institutions.stream()
        .collect(Collectors.toMap(Institution::getId, Institution::getName));
    List<Progress> allProgress = progressRepo.findAll();

    Set<Long> requiredDocIds = docs.stream()
        .filter(d -> d.getActive() && d.getRequired())
        .map(Doc::getId).collect(Collectors.toSet());
    int requiredTotal = requiredDocIds.size();
    Map<Long, Set<Long>> requiredDocsByIsland = docs.stream()
        .filter(d -> d.getActive() && d.getRequired())
        .collect(Collectors.groupingBy(Doc::getIslandId,
            Collectors.mapping(Doc::getId, Collectors.toSet())));

    Map<Long, List<Progress>> progByUser = allProgress.stream()
        .collect(Collectors.groupingBy(Progress::getUserId));
    Map<Long, Set<Long>> completedByUser = new HashMap<>();
    Map<Long, Instant> lastReadByUser = new HashMap<>();
    for (User u : users) {
      List<Progress> ups = progByUser.getOrDefault(u.getId(), List.of());
      Set<Long> completed = ups.stream()
          .filter(p -> p.getStatus() == ProgressStatus.COMPLETED)
          .map(Progress::getDocId).collect(Collectors.toSet());
      completedByUser.put(u.getId(), completed);
      lastReadByUser.put(u.getId(), ups.stream()
          .map(Progress::getLastReadAt).filter(Objects::nonNull)
          .max(Instant::compareTo).orElse(null));
    }

    // 每员工统计
    List<UserStatsDto> userStats = new ArrayList<>();
    for (User u : users) {
      Set<Long> completed = completedByUser.get(u.getId());
      int requiredCompleted = (int) completed.stream().filter(requiredDocIds::contains).count();
      int pct = requiredTotal == 0 ? 0
          : (int) Math.round(requiredCompleted * 100.0 / requiredTotal);
      int islandsCompleted = 0;
      for (Island isl : islands) {
        Set<Long> req = requiredDocsByIsland.get(isl.getId());
        if (req != null && !req.isEmpty() && completed.containsAll(req)) islandsCompleted++;
      }
      userStats.add(new UserStatsDto(u.getId(), u.getName(), u.getUsername(), requiredTotal,
          requiredCompleted, pct, lastReadByUser.get(u.getId()), islandsCompleted, islands.size()));
    }
    userStats.sort(Comparator.comparingInt(UserStatsDto::completionPct).reversed()
        .thenComparing(UserStatsDto::name));

    // 概览
    long totalLearners = users.size();
    long completedAll = userStats.stream()
        .filter(u -> u.requiredTotal() > 0 && u.completionPct() == 100).count();
    int avgPct = totalLearners == 0 ? 0
        : (int) Math.round(userStats.stream().mapToInt(UserStatsDto::completionPct).average().orElse(0));

    // 每小岛完成率
    List<IslandCompletionDto> islandCompletions = new ArrayList<>();
    for (Island isl : islands) {
      Set<Long> req = requiredDocsByIsland.get(isl.getId());
      long completedUsers = (req == null || req.isEmpty()) ? 0
          : users.stream().filter(u -> completedByUser.get(u.getId()).containsAll(req)).count();
      int pct = totalLearners == 0 ? 0
          : (int) Math.round(completedUsers * 100.0 / totalLearners);
      islandCompletions.add(new IslandCompletionDto(isl.getId(), isl.getName(),
          instName.getOrDefault(isl.getInstitutionId(), ""), completedUsers, totalLearners, pct));
    }

    StatsOverviewDto overview = new StatsOverviewDto(userRepo.count(), totalLearners,
        completedAll, avgPct, islandCompletions);
    return new Snapshot(userStats, overview);
  }

  private record Snapshot(List<UserStatsDto> userStats, StatsOverviewDto overview) {}
}
