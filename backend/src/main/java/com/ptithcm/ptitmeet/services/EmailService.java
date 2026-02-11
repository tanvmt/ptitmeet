package com.ptithcm.ptitmeet.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public void sendPasswordResetEmail(String toEmail, String fullName, String resetToken) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject("PTITMEET - Khôi phục mật khẩu");

            String resetLink = frontendUrl + "/reset-password?token=" + resetToken;

            String emailBody = String.format(
                    "Xin chào %s,\n\n" +
                            "Chúng tôi nhận được yêu cầu đặt lại mật khẩu cho tài khoản PTITMEET của bạn.\n\n" +
                            "Vui lòng nhấp vào link sau để đặt lại mật khẩu:\n%s\n\n" +
                            "Link này sẽ hết hạn sau 1 giờ.\n\n" +
                            "Nếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.\n\n" +
                            "Trân trọng,\n" +
                            "PTITMEET Team",
                    fullName,
                    resetLink);

            message.setText(emailBody);
            mailSender.send(message);

        } catch (Exception e) {
            throw new  RuntimeException("Lỗi khi gửi email reset password đến: "+ toEmail);
        }
    }
}
