package com.boardify.boardify_service.auth.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
@RequiredArgsConstructor
@Service
public class EmailService {

    private final JavaMailSender mailSender;

    // Use @Value to inject your frontend URL from properties
    @Value("${app.frontend.url:http://localhost:5173}")
    private String frontendUrl;

    @Async // Run in background so the API doesn't hang
    public void sendPasswordResetEmail(String toEmail, String token) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom("noreply@boardify.com");
        message.setTo(toEmail);
        message.setSubject("Reset your Boardify Password");

        // Construct the link pointing to your React Frontend
        String link = frontendUrl + "/reset-password?token=" + token;

        message.setText("Click the link below to reset your password:\n\n" + link +
                "\n\nThis link expires in 15 minutes.");

        mailSender.send(message);
    }
}
