package com.ptithcm.ptitmeet.controllers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ptithcm.ptitmeet.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.dto.user.UserResponse;
import com.ptithcm.ptitmeet.services.AuthService;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    @Test
    void registerShouldReturnCreatedResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .email("demo@example.com")
                .password("password123")
                .fullName("Demo User")
                .build();
        UserResponse user = UserResponse.builder()
                .userId(UUID.randomUUID())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .build();

        when(authService.register(request)).thenReturn(user);

        var response = authController.register(request);

        assertEquals(HttpStatus.CREATED, response.getStatusCode());
        assertEquals(1000, response.getBody().getCode());
        assertEquals("Đăng ký tài khoản thành công", response.getBody().getMessage());
        assertEquals(user, response.getBody().getData());
    }

    @Test
    void loginShouldReturnSuccessPayload() {
        LoginRequest request = LoginRequest.builder()
                .email("demo@example.com")
                .password("password123")
                .build();
        AuthResponse authResponse = AuthResponse.builder()
                .user(UserResponse.builder()
                        .userId(UUID.randomUUID())
                        .email(request.getEmail())
                        .fullName("Demo User")
                        .build())
                .build();
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(authService.login(request, response)).thenReturn(authResponse);

        var httpResponse = authController.login(request, response);

        assertEquals(HttpStatus.OK, httpResponse.getStatusCode());
        assertEquals(1000, httpResponse.getBody().getCode());
        assertEquals(authResponse, httpResponse.getBody().getData());
        verify(authService).login(request, response);
    }
}
