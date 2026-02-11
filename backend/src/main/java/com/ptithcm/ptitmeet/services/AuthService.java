package com.ptithcm.ptitmeet.services;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.ptithcm.ptitmeet.config.JwtTokenProvider;
import com.ptithcm.ptitmeet.dto.auth.AuthResponse;
import com.ptithcm.ptitmeet.dto.auth.ForgotPasswordRequest;
import com.ptithcm.ptitmeet.dto.auth.GoogleLoginRequest;
import com.ptithcm.ptitmeet.dto.auth.LoginRequest;
import com.ptithcm.ptitmeet.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.dto.auth.UserResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.ptithcm.ptitmeet.entity.enums.AuthProvider;
import com.ptithcm.ptitmeet.entity.mysql.User;
import com.ptithcm.ptitmeet.exception.AppException;
import com.ptithcm.ptitmeet.repositories.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final EmailService emailService;

    @Value("${google.client-id}")
    private String googleClientId;

    private final Map<String, String> resetTokenStore = new HashMap<>();

    @Transactional
    public UserResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email đã được sử dụng");
        }
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .authProvider(AuthProvider.LOCAL)
                .build();

        user = userRepository.save(user);

        return mapToUserResponse(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request, HttpServletResponse response) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.BAD_REQUEST, "Email hoặc mật khẩu không đúng"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email hoặc mật khẩu không đúng");
        }

        generateTokensAndSetCookies(user, response);

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request, HttpServletResponse response) {
        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(),
                    GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(request.getIdToken());
            if (idToken == null) {
                throw new AppException(HttpStatus.UNAUTHORIZED, "Google ID Token không hợp lệ");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String providerId = payload.getSubject();
            String email = payload.getEmail();
            String name = (String) payload.get("name");
            String pictureUrl = (String) payload.get("picture");

            User user = userRepository.findByProviderId(providerId)
                    .orElseGet(() -> {
                        return userRepository.findByEmail(email)
                                .map(existingUser -> {
                                    existingUser.setAuthProvider(AuthProvider.GOOGLE);
                                    existingUser.setProviderId(providerId);
                                    if (existingUser.getAvatarUrl() == null) {
                                        existingUser.setAvatarUrl(pictureUrl);
                                    }
                                    return userRepository.save(existingUser);
                                })
                                .orElseGet(() -> {
                                    User newUser = User.builder()
                                            .email(email)
                                            .fullName(name)
                                            .avatarUrl(pictureUrl)
                                            .authProvider(AuthProvider.GOOGLE)
                                            .providerId(providerId)
                                            .build();
                                    return userRepository.save(newUser);
                                });
                    });

            generateTokensAndSetCookies(user, response);

            return AuthResponse.builder()
                    .user(mapToUserResponse(user))
                    .build();
        } catch (Exception e) {
            if (e instanceof AppException) {
                throw (AppException) e;
            }
            throw new AppException(HttpStatus.UNAUTHORIZED, "Không thể xác thực Google token");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = getCookieValue(request, "refresh_token");

        if (refreshToken == null || !jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        if (!"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token không phải là refresh token");
        }
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        // Tạo access token mới, refresh token giữ nguyên
        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());

        // Cập nhật lại cookie access token
        addCookie(response, "access_token", newAccessToken, jwtTokenProvider.getAccessTokenExpiration() / 1000);

        return AuthResponse.builder()
                .user(mapToUserResponse(user))
                .build();
    }

    public void logout(HttpServletResponse response) {
        addCookie(response, "access_token", "", 0);
        addCookie(response, "refresh_token", "", 0);
    }

    private void generateTokensAndSetCookies(User user, HttpServletResponse response) {
        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        addCookie(response, "access_token", accessToken, jwtTokenProvider.getAccessTokenExpiration() / 1000);
        addCookie(response, "refresh_token", refreshToken, jwtTokenProvider.getRefreshTokenExpiration() / 1000);
    }

    private void addCookie(HttpServletResponse response, String name, String value, long maxAge) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true); // Chỉ gửi qua HTTPS
        cookie.setPath("/");
        cookie.setMaxAge((int) maxAge);
        cookie.setAttribute("SameSite", "Strict"); // Chống CSRF
        response.addCookie(cookie);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (name.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "Email không tồn tại trong hệ thống"));

        String resetToken = UUID.randomUUID().toString();
        resetTokenStore.put(resetToken, user.getEmail());

        emailService.sendPasswordResetEmail(user.getEmail(), user.getFullName(), resetToken);

    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {

        String email = resetTokenStore.get(request.getToken());
        if (email == null) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token không hợp lệ hoặc đã hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        resetTokenStore.remove(request.getToken());
    }

    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .build();
    }
}
