package com.gmnl.orientation.common;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(GlobalExceptionHandler.class);

  public static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ApiError(code, message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
  }

  // @Valid 校验失败（如 @NotBlank）→ 400，返回首个字段错误，而非被兜底吞成 500。
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiError> validation(MethodArgumentNotValidException e) {
    FieldError fe = e.getBindingResult().getFieldError();
    String message = fe != null ? fe.getField() + ": " + fe.getDefaultMessage() : "参数校验失败";
    return error(HttpStatus.BAD_REQUEST, "VALIDATION", message);
  }

  // 唯一约束冲突等数据完整性问题 → 400，提示编码/名称可能重复。
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiError> dataIntegrity(DataIntegrityViolationException e) {
    return error(HttpStatus.BAD_REQUEST, "DATA_CONFLICT", "数据冲突，编码或名称可能重复");
  }

  @ExceptionHandler(com.gmnl.orientation.user.AuthService.InvalidCredentialsException.class)
  public ResponseEntity<ApiError> invalidCredentials(com.gmnl.orientation.user.AuthService.InvalidCredentialsException e) {
    return error(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS", e.getMessage());
  }

  @ExceptionHandler(com.gmnl.orientation.user.AuthService.LoginLockedException.class)
  public ResponseEntity<ApiError> loginLocked(com.gmnl.orientation.user.AuthService.LoginLockedException e) {
    return error(HttpStatus.TOO_MANY_REQUESTS, "LOGIN_LOCKED", e.getMessage());
  }

  // 透传 ResponseStatusException 的状态码（如 404/403），避免被下方兜底吞成 500。
  // 比 @ExceptionHandler(Exception.class) 更具体，Spring 会优先匹配。
  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiError> responseStatus(ResponseStatusException e) {
    HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
    if (status == null) status = HttpStatus.INTERNAL_SERVER_ERROR;
    String code = status == HttpStatus.NOT_FOUND ? "NOT_FOUND"
        : status == HttpStatus.FORBIDDEN ? "FORBIDDEN"
        : status.name();
    String message = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
    return error(status, code, message);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> internal(Exception e) {
    log.error("Unhandled exception", e);
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "服务器内部错误");
  }
}
