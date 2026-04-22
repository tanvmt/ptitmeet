package com.ptithcm.ptitmeet.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.ptithcm.ptitmeet.dto.user.UpdateProfileRequest;
import com.ptithcm.ptitmeet.entity.enums.AuthProvider;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void getProfileShouldReturnMappedUser() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("demo@example.com")
                .fullName("Demo User")
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        var response = userService.getProfile(userId);

        assertEquals(userId, response.getUserId());
        assertEquals("demo@example.com", response.getEmail());
        assertEquals("LOCAL", response.getAuthProvider());
    }

    @Test
    void updateProfileShouldIgnoreBlankFullNameAndPersistAvatar() {
        UUID userId = UUID.randomUUID();
        User user = User.builder()
                .userId(userId)
                .email("demo@example.com")
                .fullName("Old Name")
                .avatarUrl(null)
                .build();
        UpdateProfileRequest request = UpdateProfileRequest.builder()
                .fullName("   ")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = userService.updateProfile(userId, request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("Old Name", userCaptor.getValue().getFullName());
        assertEquals("https://example.com/avatar.png", userCaptor.getValue().getAvatarUrl());
        assertEquals("Old Name", response.getFullName());
    }

    @Test
    void updateProfileShouldThrowWhenUserDoesNotExist() {
        UUID userId = UUID.randomUUID();
        UpdateProfileRequest request = UpdateProfileRequest.builder().build();

        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        AppException exception = assertThrows(AppException.class, () -> userService.updateProfile(userId, request));

        assertEquals(ErrorCode.USER_NOT_FOUND, exception.getErrorCode());
    }
}
