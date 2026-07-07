package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressServiceTest {

  private ProgressRepository progressRepo;
  private DocRepository docRepo;
  private IslandRepository islandRepo;
  private ProgressService service;

  @BeforeEach
  void setup() {
    progressRepo = mock(ProgressRepository.class);
    docRepo = mock(DocRepository.class);
    islandRepo = mock(IslandRepository.class);
    service = new ProgressService(progressRepo, docRepo, islandRepo);
  }

  @Test
  void upsertCreatesNewProgressWhenAbsent() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());

    var dto = service.upsert(1L, 10L, ProgressStatus.READING, 40);
    assertEquals(40, dto.progressPct());
    assertEquals(ProgressStatus.READING, dto.status());
    assertNotNull(dto.lastReadAt());
  }

  @Test
  void upsertDoesNotRegressProgressPct() {
    Progress existing = new Progress();
    existing.setUserId(1L); existing.setDocId(10L);
    existing.setStatus(ProgressStatus.READING);
    existing.setProgressPct(80);
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.of(existing));

    var dto = service.upsert(1L, 10L, ProgressStatus.READING, 20);
    // 20 < 80 → 保留 80
    assertEquals(80, dto.progressPct());
  }

  @Test
  void completeSetsFullProgressAndCompletedAt() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());

    var dto = service.complete(1L, 10L);
    assertEquals(100, dto.progressPct());
    assertEquals(ProgressStatus.COMPLETED, dto.status());
    assertNotNull(dto.completedAt());
  }

  @Test
  void islandBecomesCompletedWhenAllRequiredDocsDone() {
    Doc d1 = doc(10L, 5L, true);
    Doc d2 = doc(11L, 5L, true);
    Progress p1 = new Progress(); p1.setDocId(10L); p1.setStatus(ProgressStatus.COMPLETED);
    Progress p2 = new Progress(); p2.setDocId(11L); p2.setStatus(ProgressStatus.COMPLETED);
    when(progressRepo.findByUserId(1L)).thenReturn(List.of(p1, p2));
    when(docRepo.findAll()).thenReturn(List.of(d1, d2));
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(island(5L, 1L, 0)));

    var agg = service.getAggregate(1L);
    IslandStateViewDto view = agg.islands().get(0);
    assertEquals(IslandStatus.COMPLETED, view.status());
    assertEquals(2, view.completedCount());
    assertEquals(2, view.totalCount());
  }

  @Test
  void firstIslandUnlockedAsEntryPointNextLocked() {
    // 同一机构两个小岛(order 0、1),各一个必修文档,均未开始
    Doc d1 = doc(10L, 5L, true);
    Doc d2 = doc(11L, 6L, true);
    when(progressRepo.findByUserId(1L)).thenReturn(List.of());
    when(docRepo.findAll()).thenReturn(List.of(d1, d2));
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(
        island(5L, 1L, 0), island(6L, 1L, 1)));

    var agg = service.getAggregate(1L);
    IslandStatus s5 = statusOf(agg, 5L);
    IslandStatus s6 = statusOf(agg, 6L);
    assertEquals(IslandStatus.UNLOCKED, s5); // 入口
    assertEquals(IslandStatus.LOCKED, s6);   // 前岛未完成
  }

  @Test
  void secondIslandUnlocksWhenFirstCompleted() {
    // 小岛 5 的必修文档已完成 → 5=COMPLETED,6 解锁
    Doc d1 = doc(10L, 5L, true);
    Doc d2 = doc(11L, 6L, true);
    Progress p1 = new Progress(); p1.setDocId(10L); p1.setStatus(ProgressStatus.COMPLETED);
    when(progressRepo.findByUserId(1L)).thenReturn(List.of(p1));
    when(docRepo.findAll()).thenReturn(List.of(d1, d2));
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(
        island(5L, 1L, 0), island(6L, 1L, 1)));

    var agg = service.getAggregate(1L);
    assertEquals(IslandStatus.COMPLETED, statusOf(agg, 5L));
    assertEquals(IslandStatus.UNLOCKED, statusOf(agg, 6L)); // 前岛完成 → 解锁
  }

  private IslandStatus statusOf(ProgressAggregateDto agg, long islandId) {
    return agg.islands().stream()
        .filter(i -> i.islandId() == islandId)
        .findFirst().orElseThrow().status();
  }

  private Doc doc(long id, long islandId, boolean required) {
    Doc d = new Doc();
    d.setId(id);
    d.setIslandId(islandId);
    d.setRequired(required);
    d.setActive(true);
    return d;
  }

  private Island island(long id, long institutionId, int order) {
    Island i = new Island();
    i.setId(id);
    i.setInstitutionId(institutionId);
    i.setOrder(order);
    return i;
  }
}
