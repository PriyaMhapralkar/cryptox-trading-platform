package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.VerificationCodeRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;

@Service
public class OtpService {

    @Autowired private VerificationCodeRepository verificationCodeRepository;
    @Autowired private EmailService emailService;

    public void generateAndSendOtp(User user, VerificationType type) {
        String otp = String.valueOf(100000 + new Random().nextInt(900000)); // 6-digit OTP

        VerificationCode code = VerificationCode.builder()
                .otp(otp)
                .user(user)
                .email(user.getEmail())
                .mobile(user.getMobile())
                .verificationType(type)
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .isUsed(false)
                .build();

        verificationCodeRepository.save(code);
        emailService.sendOtpEmail(user.getEmail(), otp, type.name());
    }

    public boolean verifyOtp(User user, VerificationType type, String submittedOtp) {
        VerificationCode code = verificationCodeRepository
                .findTopByUserIdAndVerificationTypeAndIsUsedFalseOrderByIdDesc(user.getId(), type)
                .orElseThrow(() -> new RuntimeException("No OTP found. Please request a new one."));

        if (code.isUsed()) {
            throw new RuntimeException("OTP already used.");
        }
        if (code.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP expired. Please request a new one.");
        }
        if (!code.getOtp().equals(submittedOtp)) {
            throw new RuntimeException("Invalid OTP.");
        }

        code.setUsed(true);
        verificationCodeRepository.save(code);
        return true;
    }
}