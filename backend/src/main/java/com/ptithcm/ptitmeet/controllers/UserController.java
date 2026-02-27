package com.ptithcm.ptitmeet.controllers;

import com.ptithcm.ptitmeet.dto.ApiResponse;
import com.ptithcm.ptitmeet.dto.auth.UserResponse;
import com.ptithcm.ptitmeet.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.services.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser(Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        UserResponse user = userService.getProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.success(user, "Lấy thông tin người dùng hiện tại thành công"));
    }

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(Authentication authentication) {

        UUID userId = UUID.fromString(authentication.getName());

        UserResponse user = userService.getProfile(userId);

        return ResponseEntity.ok(
                ApiResponse.success(user, "Lấy thông tin thành công"));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Authentication authentication,
            @Valid @RequestBody UpdateProfileRequest request) {

        UUID userId = UUID.fromString(authentication.getName());

        UserResponse user = userService.updateProfile(userId, request);

        return ResponseEntity.ok(
                ApiResponse.success(user, "Cập nhật thông tin thành công"));
    }
}
