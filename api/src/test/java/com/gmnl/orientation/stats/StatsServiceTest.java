package com.gmnl.orientation.stats;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.InstitutionRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import com.gmnl.orientation.progress.Progress;
import com.gmnl.orientation.progress.ProgressRepository;
import com.gmnl.orientation.progress.ProgressStatus;
import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import com.gmnl.orientation.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatsServiceTest {

  private UserRepository userRepo;
  private DocRepository docRepo;
  private IslandRepository islandRepo;
  private InstitutionRepository institutionRepo;
  private ProgressRepository progressRepo;
  private StatsService service;

  @BeforeEach
  void setup() {
    userRepo = mock(UserRepository.class);
    docRepo = mock(DocRepository.class);
    islandRepo = mock(IslandRepository.class);
    institutionRepo = mock(InstitutionRepository.class);
    progressRepo = mock(ProgressRepository.class);
    service = new StatsService(userRepo, docRepo, islandRepo, institutionRepo, progressRepo);
  }

  @Test
  void computesPerUserCompletionAndOverview() {
    User u1 = user(1L, "张三");
    User u2 = user(2L, "李四");
    when(userRepo.findByRole(UserRole.USER)).thenReturn(List.of(u1, u2));
    when(userRepo.count()).thenReturn(2L);
    // 2 份必修文档同属小岛 5
    when(docRepo.findAll()).thenReturn(List.of(requiredDoc(10L, 5L), requiredDoc(11L, 5L)));
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(island(5L, 1L)));
    when(institutionRepo.findAllByOrderByOrderAsc()).thenReturn(List.of());
    // 张三完成两份，李四只读了一份（未完成）
    when(progressRepo.findAll()).thenReturn(List.of(
        progress(1L, 10L, ProgressStatus.COMPLETED),
        progress(1L, 11L, ProgressStatus.COMPLETED),
        progress(2L, 10L, ProgressStatus.READING)));

    List<UserStatsDto> users = service.userStats();
    assertEquals(2, users.size());
    // 按完成率降序，张三在前
    UserStatsDto first = users.get(0);
    assertEquals(1L, first.userId());
    assertEquals(100, first.completionPct());
    assertEquals(2, first.requiredCompleted());
    assertEquals(1, first.islandsCompleted());
    UserStatsDto second = users.get(1);
    assertEquals(0, second.completionPct());

    StatsOverviewDto ov = service.overview();
    assertEquals(2, ov.totalLearners());
    assertEquals(1, ov.completedAllRequired());
    assertEquals(50, ov.avgCompletionPct()); // (100 + 0) / 2
    assertEquals(1, ov.islands().size());
    assertEquals(50, ov.islands().get(0).completionPct()); // 1/2 学员完成小岛 5
  }

  @Test
  void emptyLearnersYieldsZeroOverview() {
    when(userRepo.findByRole(UserRole.USER)).thenReturn(List.of());
    when(userRepo.count()).thenReturn(1L);
    when(docRepo.findAll()).thenReturn(List.of());
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of());
    when(institutionRepo.findAllByOrderByOrderAsc()).thenReturn(List.of());
    when(progressRepo.findAll()).thenReturn(List.of());

    assertEquals(List.of(), service.userStats());
    StatsOverviewDto ov = service.overview();
    assertEquals(0, ov.totalLearners());
    assertEquals(0, ov.avgCompletionPct());
    assertEquals(1, ov.totalUsers());
  }

  private User user(long id, String name) {
    User u = new User();
    u.setId(id);
    u.setName(name);
    u.setUsername("u" + id);
    u.setRole(UserRole.USER);
    return u;
  }

  private Doc requiredDoc(long id, long islandId) {
    Doc d = new Doc();
    d.setId(id);
    d.setIslandId(islandId);
    d.setRequired(true);
    d.setActive(true);
    return d;
  }

  private Island island(long id, long institutionId) {
    Island isl = new Island();
    isl.setId(id);
    isl.setInstitutionId(institutionId);
    return isl;
  }

  private Progress progress(long userId, long docId, ProgressStatus status) {
    Progress p = new Progress();
    p.setUserId(userId);
    p.setDocId(docId);
    p.setStatus(status);
    p.setProgressPct(status == ProgressStatus.COMPLETED ? 100 : 10);
    return p;
  }
}
