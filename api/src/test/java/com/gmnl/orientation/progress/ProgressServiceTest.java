package com.gmnl.orientation.progress;

import com.gmnl.orientation.content.Doc;
import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.Island;
import com.gmnl.orientation.content.IslandRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProgressServiceTest {

  private ProgressRepository progressRepo;
  private IslandStateRepository islandStateRepo;
  private DocRepository docRepo;
  private IslandRepository islandRepo;
  private ProgressService service;

  @BeforeEach
  void setup() {
    progressRepo = mock(ProgressRepository.class);
    islandStateRepo = mock(IslandStateRepository.class);
    docRepo = mock(DocRepository.class);
    islandRepo = mock(IslandRepository.class);
    service = new ProgressService(progressRepo, islandStateRepo, docRepo, islandRepo);
  }

  @Test
  void upsertCreatesNewProgressWhenAbsent() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of());
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

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
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of(existing));
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

    var dto = service.upsert(1L, 10L, ProgressStatus.READING, 20);
    // 20 < 80 → 保留 80
    assertEquals(80, dto.progressPct());
  }

  @Test
  void completeSetsFullProgressAndCompletedAt() {
    when(progressRepo.findById(new ProgressId(1L, 10L))).thenReturn(Optional.empty());
    Doc doc = doc(10L, 5L, true);
    when(docRepo.findById(10L)).thenReturn(Optional.of(doc));
    when(docRepo.findByIslandIdAndActiveTrueOrderByOrderAsc(5L)).thenReturn(List.of(doc));
    when(progressRepo.findByUserId(1L)).thenReturn(List.of());
    when(islandStateRepo.findById(new IslandStateId(1L, 5L))).thenReturn(Optional.empty());

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
    Island island = new Island(); island.setId(5L);
    when(islandRepo.findAllByOrderByOrderAsc()).thenReturn(List.of(island));

    var agg = service.getAggregate(1L);
    IslandStateViewDto view = agg.islands().get(0);
    assertEquals(IslandStatus.COMPLETED, view.status());
    assertEquals(2, view.completedCount());
    assertEquals(2, view.totalCount());
  }

  private Doc doc(long id, long islandId, boolean required) {
    Doc d = new Doc();
    d.setId(id);
    d.setIslandId(islandId);
    d.setRequired(required);
    d.setActive(true);
    return d;
  }
}
