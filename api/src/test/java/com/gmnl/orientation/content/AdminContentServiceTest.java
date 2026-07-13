package com.gmnl.orientation.content;

import com.gmnl.orientation.progress.ProgressRepository;
import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AdminContentServiceTest {

  private DocRepository docRepo;
  private InstitutionRepository institutionRepo;
  private IslandRepository islandRepo;
  private UserRepository userRepo;
  private ProgressRepository progressRepo;
  private DocFileStorage storage;
  private AdminContentService service;

  @BeforeEach
  void setup() {
    docRepo = mock(DocRepository.class);
    institutionRepo = mock(InstitutionRepository.class);
    islandRepo = mock(IslandRepository.class);
    userRepo = mock(UserRepository.class);
    progressRepo = mock(ProgressRepository.class);
    storage = mock(DocFileStorage.class);
    service = new AdminContentService(docRepo, institutionRepo, islandRepo, userRepo, progressRepo, storage);
  }

  @Test
  void uploadSavesPathTypeAndAudit() throws Exception {
    Doc doc = doc(1L);
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
    when(storage.store(eq(1L), anyString(), any())).thenReturn("docs/1/123_brand.pdf");
    when(userRepo.findById(7L)).thenReturn(Optional.of(user(7L, "张三")));
    MultipartFile file = file("品牌手册.pdf", "pdf-content");

    AdminDocDto dto = service.uploadFile(1L, file, 7L);

    assertEquals("pdf", dto.fileType());
    assertEquals(FileTypeSupport.Category.DOCUMENT, dto.fileCategory());
    assertNotNull(dto.uploadedAt());
    assertEquals("张三", dto.uploadedByName());
    verify(docRepo).save(doc);
    verify(storage, never()).delete(anyString()); // 无旧文件，不清理
  }

  @Test
  void uploadReplacesAndDeletesOldFile() throws Exception {
    Doc doc = doc(1L);
    doc.setFilePath("docs/1/old.pdf");
    doc.setFileType("pdf");
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
    when(storage.store(eq(1L), anyString(), any())).thenReturn("docs/1/999_new.pdf");
    when(userRepo.findById(7L)).thenReturn(Optional.of(user(7L, "张三")));
    MultipartFile file = file("新版.pdf", "x");

    service.uploadFile(1L, file, 7L);

    assertEquals("docs/1/999_new.pdf", doc.getFilePath());
    verify(storage).delete("docs/1/old.pdf"); // 旧文件被清理
  }

  @Test
  void uploadRejectsUnsupportedExtension() throws Exception {
    MultipartFile file = file("hack.exe", "x");
    assertThrows(IllegalArgumentException.class, () -> service.uploadFile(1L, file, 7L));
    verify(storage, never()).store(anyLong(), anyString(), any());
  }

  @Test
  void uploadRejectsEmptyFile() throws Exception {
    MultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);
    assertThrows(IllegalArgumentException.class, () -> service.uploadFile(1L, file, 7L));
    verify(storage, never()).store(anyLong(), anyString(), any());
  }

  @Test
  void uploadThrowsNotFoundWhenDocMissing() throws Exception {
    when(docRepo.findById(99L)).thenReturn(Optional.empty());
    MultipartFile file = file("x.pdf", "x");
    assertThrows(ResponseStatusException.class, () -> service.uploadFile(99L, file, 7L));
  }

  @Test
  void deleteFileClearsFieldsAndRemovesPhysicalFile() {
    Doc doc = doc(1L);
    doc.setFilePath("docs/1/old.mp4");
    doc.setFileType("mp4");
    doc.setFileUploadedBy(7L);
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));

    AdminDocDto dto = service.deleteFile(1L);

    assertNull(doc.getFilePath());
    assertNull(doc.getFileType());
    assertNull(doc.getFileUploadedBy());
    assertEquals(FileTypeSupport.Category.OTHER, dto.fileCategory());
    verify(storage).delete("docs/1/old.mp4");
    verify(docRepo).save(doc);
  }

  @Test
  void deleteFileIsIdempotentWhenNoFile() {
    Doc doc = doc(1L);
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
    service.deleteFile(1L);
    verify(storage, never()).delete(anyString());
  }

  @Test
  void listDocsResolvesUploaderNames() {
    Doc d1 = doc(1L);
    Doc d2 = doc(2L);
    d2.setFileUploadedBy(7L);
    when(docRepo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(d1, d2));
    when(userRepo.findAllById(any())).thenReturn(List.of(user(7L, "张三")));

    List<AdminDocDto> result = service.listDocs();

    assertNull(result.get(0).uploadedByName());
    assertEquals("张三", result.get(1).uploadedByName());
  }

  // ===== 阶段 5 内容 CRUD =====

  @Test
  void createInstitutionSavesAndReturnsDto() {
    InstitutionDto dto = service.createInstitution(new InstitutionUpsertRequest("TEST", "测试机构", 5));
    assertEquals("TEST", dto.key());
    assertEquals("测试机构", dto.name());
    assertEquals(5, dto.order());
    verify(institutionRepo).save(any(Institution.class));
  }

  @Test
  void deleteInstitutionRefusesWhenHasIslands() {
    when(institutionRepo.existsById(1L)).thenReturn(true);
    when(islandRepo.countByInstitutionId(1L)).thenReturn(3L);
    assertThrows(IllegalArgumentException.class, () -> service.deleteInstitution(1L));
    verify(institutionRepo, never()).deleteById(anyLong());
  }

  @Test
  void deleteInstitutionSucceedsWhenNoIslands() {
    when(institutionRepo.existsById(1L)).thenReturn(true);
    when(islandRepo.countByInstitutionId(1L)).thenReturn(0L);
    service.deleteInstitution(1L);
    verify(institutionRepo).deleteById(1L);
  }

  @Test
  void deleteIslandRefusesWhenHasDocs() {
    when(islandRepo.existsById(10L)).thenReturn(true);
    when(docRepo.countByIslandId(10L)).thenReturn(2L);
    assertThrows(IllegalArgumentException.class, () -> service.deleteIsland(10L));
    verify(islandRepo, never()).deleteById(anyLong());
  }

  @Test
  void createDocValidatesIslandAndInstitutionMatch() {
    Island isl = new Island();
    isl.setId(10L);
    isl.setInstitutionId(1L);
    when(islandRepo.findById(10L)).thenReturn(Optional.of(isl));
    DocUpsertRequest req = new DocUpsertRequest("新文档", "cat", 1L, 10L, true, 0, true);
    AdminDocDto dto = service.createDoc(req);
    assertEquals("新文档", dto.title());
    assertTrue(dto.required());
    verify(docRepo).save(any(Doc.class));
  }

  @Test
  void createDocRejectsInstitutionMismatch() {
    Island isl = new Island();
    isl.setId(10L);
    isl.setInstitutionId(1L);
    when(islandRepo.findById(10L)).thenReturn(Optional.of(isl));
    DocUpsertRequest req = new DocUpsertRequest("新文档", null, 2L, 10L, true, 0, true);
    assertThrows(IllegalArgumentException.class, () -> service.createDoc(req));
    verify(docRepo, never()).save(any(Doc.class));
  }

  @Test
  void deleteDocRowRefusesWhenHasProgress() {
    Doc doc = doc(1L);
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
    when(progressRepo.countByDocId(1L)).thenReturn(5L);
    assertThrows(IllegalArgumentException.class, () -> service.deleteDocRow(1L));
    verify(docRepo, never()).delete(any(Doc.class));
  }

  @Test
  void deleteDocRowDeletesFileAndDocWhenNoProgress() {
    Doc doc = doc(1L);
    doc.setFilePath("docs/1/x.pdf");
    doc.setFileType("pdf");
    when(docRepo.findById(1L)).thenReturn(Optional.of(doc));
    when(progressRepo.countByDocId(1L)).thenReturn(0L);
    service.deleteDocRow(1L);
    verify(storage).delete("docs/1/x.pdf");
    verify(docRepo).delete(doc);
  }

  private Doc doc(long id) {
    Doc d = new Doc();
    d.setId(id);
    d.setTitle("文档" + id);
    d.setInstitutionId(1L);
    d.setIslandId(10L);
    d.setRequired(true);
    d.setOrder(0);
    d.setActive(true);
    return d;
  }

  private User user(long id, String name) {
    User u = new User();
    u.setId(id);
    u.setName(name);
    return u;
  }

  private MultipartFile file(String filename, String content) {
    // 用 byte[] 构造器，避免 InputStream 构造器的受检 IOException
    return new MockMultipartFile("file", filename, "application/octet-stream", content.getBytes());
  }
}
