package com.cryptox.backend.controller;

import com.cryptox.backend.dto.*;

import com.cryptox.backend.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/register")
    public AuthResponse register(@RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@RequestBody LoginRequest request) {
        return authService.login(request);
    }
    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(@RequestBody VerifyOtpRequest request) {
        return authService.verifyLoginOtp(request);
    }
}