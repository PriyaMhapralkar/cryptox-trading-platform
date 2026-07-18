package com.cryptox.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendOtpEmail(String toEmail, String otp, String purpose) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject("CryptoX - Your OTP Code");
        message.setText("Your OTP for " + purpose + " is: " + otp
                + "\n\nThis code expires in 5 minutes. Do not share it with anyone.");
        mailSender.send(message);
    }
}