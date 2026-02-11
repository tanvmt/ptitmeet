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
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.login(request, response);
    ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Đăng nhập thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(apiResponse);
  }

  @PostMapping("/google")
  public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.loginWithGoogle(request, response);

    ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Đăng nhập Google thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(apiResponse);
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
      HttpServletRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.refreshToken(request, response);

    ApiResponse<AuthResponse> apiResponse = ApiResponse.<AuthResponse>builder()
        .status(HttpStatus.OK.value())
        .message("Làm mới token thành công")
        .data(authResponse)
        .build();

    return ResponseEntity.ok(apiResponse);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {

    authService.logout(response);

    ApiResponse<Void> apiResponse = ApiResponse.<Void>builder()
        .status(HttpStatus.OK.value())
        .message("Đăng xuất thành công")
        .build();

    return ResponseEntity.ok(apiResponse);
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
