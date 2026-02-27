package com.ptithcm.ptitmeet.services;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
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

    @Async 
    public void sendMeetingInvite(String toEmail, String meetingCode, String title, LocalDateTime startTime, String hostName) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("Lời mời tham gia cuộc họp: " + title);

            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy");
            String formattedTime = startTime.format(formatter);
            String joinLink = "http://localhost:5173/waiting-room/" + meetingCode;

            String htmlContent = String.format(
                "<div style='font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;'>" +
                "   <h2 style='color: #0b5cbe;'>Bạn có một lời mời họp!</h2>" +
                "   <p>Xin chào,</p>" +
                "   <p><strong>%s</strong> đã mời bạn tham gia cuộc họp trực tuyến trên hệ thống PTITMeet.</p>" +
                "   <div style='background-color: #f4f7f6; padding: 15px; border-radius: 8px; margin: 20px 0;'>" +
                "       <p><strong>Chủ đề:</strong> %s</p>" +
                "       <p><strong>Thời gian:</strong> %s</p>" +
                "       <p><strong>Mã phòng:</strong> <span style='color: #e63946; font-weight: bold;'>%s</span></p>" +
                "   </div>" +
                "   <a href='%s' style='display: inline-block; padding: 12px 24px; background-color: #137fec; color: white; text-decoration: none; font-weight: bold; border-radius: 5px;'>Tham Gia Cuộc Họp</a>" +
                "   <p style='margin-top: 20px; font-size: 12px; color: #888;'>Hoặc copy đường link này dán vào trình duyệt: %s</p>" +
                "</div>", 
                hostName, title, formattedTime, meetingCode, joinLink, joinLink
            );

            helper.setText(htmlContent, true);
            mailSender.send(message);

        } catch (MessagingException e) {
            System.err.println("Lỗi gửi email đến " + toEmail + ": " + e.getMessage());
        }
    }
}
