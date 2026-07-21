package com.gmnl.orientation.seed;

import com.gmnl.orientation.content.DocRepository;
import com.gmnl.orientation.content.InstitutionRepository;
import com.gmnl.orientation.content.IslandRepository;
import com.gmnl.orientation.user.User;
import com.gmnl.orientation.user.UserRepository;
import com.gmnl.orientation.user.UserRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataInitializerTest {

  private InstitutionRepository institutionRepo;
  private IslandRepository islandRepo;
  private DocRepository docRepo;
  private UserRepository userRepo;
  private PasswordEncoder passwordEncoder;

  @BeforeEach
  void setup() {
    institutionRepo = mock(InstitutionRepository.class);
    islandRepo = mock(IslandRepository.class);
    docRepo = mock(DocRepository.class);
    userRepo = mock(UserRepository.class);
    passwordEncoder = mock(PasswordEncoder.class);
    when(passwordEncoder.encode(anyString())).thenAnswer(inv -> "hash(" + inv.getArgument(0) + ")");
  }

  private DataInitializer initializer(String adminPassword) {
    return new DataInitializer(institutionRepo, islandRepo, docRepo, userRepo,
        passwordEncoder, adminPassword);
  }

  @Test
  void usesConfiguredAdminPassword() {
    when(userRepo.count()).thenReturn(0L);
    initializer("S3cure-Admin-Pass").run();

    verify(passwordEncoder).encode("S3cure-Admin-Pass");
    ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
    verify(userRepo).save(captor.capture());
    User admin = captor.getValue();
    assertEquals("admin", admin.getUsername());
    assertEquals(UserRole.ADMIN, admin.getRole());
    assertEquals("hash(S3cure-Admin-Pass)", admin.getPasswordHash());
  }

  @Test
  void generatesRandomPasswordWhenUnset() {
    when(userRepo.count()).thenReturn(0L);
    initializer("").run();

    ArgumentCaptor<String> raw = ArgumentCaptor.forClass(String.class);
    verify(passwordEncoder).encode(raw.capture());
    String generated = raw.getValue();
    assertNotNull(generated);
    assertTrue(generated.length() >= 12, "随机密码至少 12 位");
    assertNotEquals("admin12345", generated);
  }

  @Test
  void blankConfiguredPasswordFallsBackToRandom() {
    String generated = DataInitializer.resolveAdminInitialPassword("   ");
    assertNotNull(generated);
    assertTrue(generated.length() >= 12);
  }

  @Test
  void skipsWhenUsersExist() {
    when(userRepo.count()).thenReturn(1L);
    initializer(null).run();
    verify(userRepo, never()).save(any());
    verify(passwordEncoder, never()).encode(anyString());
  }
}
