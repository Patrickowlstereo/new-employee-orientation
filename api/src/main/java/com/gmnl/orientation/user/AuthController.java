package com.gmnl.orientation.user;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

  private final AuthService authService;
  private final UserRepository userRepo;
  private final CurrentUserResolver currentUser;

  public AuthController(AuthService authService, UserRepository userRepo, CurrentUserResolver currentUser) {
    this.authService = authService;
    this.userRepo = userRepo;
    this.currentUser = currentUser;
  }

  @PostMapping("/login")
  public AuthService.LoginResult login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req.username(), req.password());
  }

  @GetMapping("/me")
  public UserDto me() {
    return userRepo.findById(currentUser.userId())
        .map(UserDto::from)
        .orElseThrow(() -> new IllegalStateException("token 用户不存在"));
  }

  @PutMapping("/password")
  public ResponseEntity<Map<String, String>> changePassword(@Valid @RequestBody ChangePasswordRequest req) {
    authService.changePassword(currentUser.userId(), req.oldPassword(), req.newPassword());
    return ResponseEntity.ok(Map.of("message", "密码已更新"));
  }
}
