package com.ptithcm.ptitmeet.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import com.ptithcm.ptitmeet.config.JwtTokenProvider;
import com.ptithcm.ptitmeet.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.entity.enums.AuthProvider;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.exception.ErrorCode;
import com.ptithcm.ptitmeet.repositories.UserRepository;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "googleClientId", "google-client-id");
    }

    @Test
    void registerShouldPersistEncodedPasswordAndReturnUserResponse() {
        RegisterRequest request = RegisterRequest.builder()
                .email("demo@example.com")
                .password("password123")
                .fullName("Demo User")
                .build();

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email(request.getEmail())
                .fullName(request.getFullName())
                .passwordHash("encoded-password")
                .authProvider(AuthProvider.LOCAL)
                .build();

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        var response = authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertEquals("demo@example.com", userCaptor.getValue().getEmail());
        assertEquals("encoded-password", userCaptor.getValue().getPasswordHash());
        assertEquals(AuthProvider.LOCAL, userCaptor.getValue().getAuthProvider());
        assertEquals(savedUser.getUserId(), response.getUserId());
    }

    @Test
    void loginShouldRejectInvalidPassword() {
        LoginRequest request = LoginRequest.builder()
                .email("demo@example.com")
                .password("wrong-password")
                .build();

        User existingUser = User.builder()
                .userId(UUID.randomUUID())
                .email(request.getEmail())
                .passwordHash("stored-hash")
                .fullName("Demo User")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(existingUser));
        when(passwordEncoder.matches(request.getPassword(), existingUser.getPasswordHash())).thenReturn(false);

        AppException exception = assertThrows(AppException.class,
                () -> authService.login(request, new MockHttpServletResponse()));

        assertEquals(ErrorCode.INVALID_LOGIN, exception.getErrorCode());
        verify(jwtTokenProvider, never()).generateAccessToken(any(UUID.class), any(String.class));
    }

    @Test
    void refreshTokenShouldIssueNewAccessTokenFromRefreshCookie() {
        UUID userId = UUID.randomUUID();
        String refreshToken = "refresh-token";
        String newAccessToken = "new-access-token";
        User user = User.builder()
                .userId(userId)
                .email("demo@example.com")
                .fullName("Demo User")
                .build();

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new jakarta.servlet.http.Cookie("refresh_token", refreshToken));
        MockHttpServletResponse response = new MockHttpServletResponse();

        when(jwtTokenProvider.validateToken(refreshToken)).thenReturn(true);
        when(jwtTokenProvider.getTokenType(refreshToken)).thenReturn("refresh");
        when(jwtTokenProvider.getUserIdFromToken(refreshToken)).thenReturn(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(jwtTokenProvider.generateAccessToken(userId, user.getEmail())).thenReturn(newAccessToken);
        when(jwtTokenProvider.getAccessTokenExpiration()).thenReturn(3_600_000L);

        AuthResponse authResponse = authService.refreshToken(request, response);

        assertNotNull(authResponse);
        assertEquals(userId, authResponse.getUser().getUserId());
        assertEquals("new-access-token", response.getCookie("access_token").getValue());
        assertEquals("refresh_token", request.getCookies()[0].getName());
    }
}
