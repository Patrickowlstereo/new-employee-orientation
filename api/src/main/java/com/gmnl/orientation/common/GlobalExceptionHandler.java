package com.gmnl.orientation.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  public static ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
    return ResponseEntity.status(status).body(new ApiError(code, message));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiError> badRequest(IllegalArgumentException e) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", e.getMessage());
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ApiError> internal(Exception e) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL", "服务器内部错误");
  }
}
