package com.cryptox.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String message;
    private String jwt;
    private boolean isTwoFactorRequired;
    private String status; // e.g. "SUCCESS", "OTP_SENT"
}