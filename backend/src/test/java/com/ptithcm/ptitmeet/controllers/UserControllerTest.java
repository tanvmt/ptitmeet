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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

import com.ptithcm.ptitmeet.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.dto.user.UserResponse;
import com.ptithcm.ptitmeet.services.UserService;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @Test
    void getCurrentUserShouldReadAuthenticationNameAsUuid() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        UserResponse response = UserResponse.builder()
                .userId(userId)
                .email("demo@example.com")
                .fullName("Demo User")
                .build();

        when(userService.getProfile(userId)).thenReturn(response);

        var httpResponse = userController.getCurrentUser(authentication);

        assertEquals(1000, httpResponse.getBody().getCode());
        assertEquals(response, httpResponse.getBody().getData());
        verify(userService).getProfile(userId);
    }

    @Test
    void updateProfileShouldDelegateToService() {
        UUID userId = UUID.randomUUID();
        Authentication authentication = new UsernamePasswordAuthenticationToken(userId.toString(), null);
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("Updated User")
                .build();
        UserResponse response = UserResponse.builder()
                .userId(userId)
                .email("demo@example.com")
                .fullName("Updated User")
                .build();

        when(userService.updateProfile(userId, request)).thenReturn(response);

        var httpResponse = userController.updateProfile(authentication, request);

        assertEquals("Cập nhật thông tin thành công", httpResponse.getBody().getMessage());
        assertEquals(response, httpResponse.getBody().getData());
    }
}
