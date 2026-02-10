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
import com.ptithcm.ptitmeet.dto.auth.RefreshTokenRequest;
import com.ptithcm.ptitmeet.dto.auth.RegisterRequest;
import com.ptithcm.ptitmeet.dto.auth.ResetPasswordRequest;
import com.ptithcm.ptitmeet.dto.auth.UserResponse;
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

    @Value("${google.client-id:}")
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
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng"));

        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Tài khoản này đã đăng ký bằng " + user.getAuthProvider()
                            + ". Vui lòng sử dụng phương thức đăng nhập tương ứng.");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Email hoặc mật khẩu không đúng");
        }

        String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .user(mapToUserResponse(user))
                .build();
    }

    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
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
                        if (userRepository.existsByEmail(email)) {
                            throw new AppException(HttpStatus.BAD_REQUEST,
                                    "Email này đã được đăng ký bằng phương thức khác");
                        }

                        User newUser = User.builder()
                                .email(email)
                                .fullName(name)
                                .avatarUrl(pictureUrl)
                                .authProvider(AuthProvider.GOOGLE)
                                .providerId(providerId)
                                .build();

                        return userRepository.save(newUser);
                    });

            String accessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getUserId());

            return AuthResponse.builder()
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .user(mapToUserResponse(user))
                    .build();
        } catch (Exception e) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Không thể xác thực Google token");
        }
    }

    @Transactional(readOnly = true)
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        log.info("Làm mới access token");

        String refreshToken = request.getRefreshToken();

        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new AppException(HttpStatus.UNAUTHORIZED, "Refresh token không hợp lệ hoặc đã hết hạn");
        }

        if (!"refresh".equals(jwtTokenProvider.getTokenType(refreshToken))) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Token không phải là refresh token");
        }
        UUID userId = jwtTokenProvider.getUserIdFromToken(refreshToken);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND, "User không tồn tại"));

        String newAccessToken = jwtTokenProvider.generateAccessToken(user.getUserId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Giữ nguyên refresh token
                .user(mapToUserResponse(user))
                .build();
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
