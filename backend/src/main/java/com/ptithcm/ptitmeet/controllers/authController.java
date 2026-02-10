package com.ptithcm.ptitmeet.controllers;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.dto.auth.*;
import com.ptithcm.ptitmeet.services.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class authController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<ApiResponse<UserResponse>> register(
      @Valid @RequestBody RegisterRequest request) {

    UserResponse user = authService.register(request);

    
    ApiResponse<UserResponse> response = ApiResponse.<UserResponse>builder()
    .status(HttpStatus.CREATED.value())
    .message("Đăng ký tài khoản thành công")
    .data(user)
    .build();
    
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest request) {

    AuthResponse authResponse = authService.login(request);
    ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Đăng nhập thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/google")
  public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request) {

    AuthResponse authResponse = authService.loginWithGoogle(request);

    ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Đăng nhập Google thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
      @Valid @RequestBody RefreshTokenRequest request) {

    AuthResponse authResponse = authService.refreshToken(request);

    ApiResponse<AuthResponse> response = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Làm mới token thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    authService.forgotPassword(request);

    ApiResponse<Void> response = ApiResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Email khôi phục đã được gửi")
        .build();

    return ResponseEntity.ok(response);
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {

    authService.resetPassword(request);

    ApiResponse<Void> response = ApiResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Mật khẩu đã được cập nhật")
        .build();

    return ResponseEntity.ok(response);
  }
}
