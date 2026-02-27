package com.ptithcm.ptitmeet.controllers;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.dto.auth.ForgotPasswordRequest;
import com.ptithcm.ptitmeet.dto.auth.GoogleLoginRequest;
import com.ptithcm.ptitmeet.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.dto.auth.UserResponse;
import com.ptithcm.ptitmeet.services.AuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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

    return ResponseEntity.status(HttpStatus.CREATED).body(
        ApiResponse.success(user, "Đăng ký tài khoản thành công"));
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AuthResponse>> login(
      @Valid @RequestBody LoginRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.login(request, response);

    return ResponseEntity.ok(
        ApiResponse.success(authResponse, "Đăng nhập thành công"));
  }

  @PostMapping("/google")
  public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
      @Valid @RequestBody GoogleLoginRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.loginWithGoogle(request, response);

    return ResponseEntity.ok(
        ApiResponse.success(authResponse, "Đăng nhập Google thành công"));
  }

  @PostMapping("/refresh-token")
  public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
      HttpServletRequest request, HttpServletResponse response) {

    AuthResponse authResponse = authService.refreshToken(request, response);

    return ResponseEntity.ok(
        ApiResponse.success(authResponse, "Làm mới token thành công"));
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(HttpServletResponse response) {

    authService.logout(response);

    return ResponseEntity.ok(
        ApiResponse.success(null, "Đăng xuất thành công"));
  }

  @PostMapping("/forgot-password")
  public ResponseEntity<ApiResponse<Void>> forgotPassword(
      @Valid @RequestBody ForgotPasswordRequest request) {

    authService.forgotPassword(request);

    return ResponseEntity.ok(
        ApiResponse.success(null, "Email khôi phục đã được gửi"));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<ApiResponse<Void>> resetPassword(
      @Valid @RequestBody ResetPasswordRequest request) {

    authService.resetPassword(request);

    return ResponseEntity.ok(
        ApiResponse.success(null, "Mật khẩu đã được cập nhật"));
  }
}
