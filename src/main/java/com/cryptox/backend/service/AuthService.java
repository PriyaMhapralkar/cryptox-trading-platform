package com.cryptox.backend.service;

import com.cryptox.backend.dto.*;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.repository.WalletRepository;
import com.cryptox.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
@Service
public class AuthService {

    @Autowired private UserRepository userRepository;
    @Autowired private WalletRepository walletRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private JwtUtil jwtUtil;
    @Autowired private OtpService otpService;

    public AuthResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new RuntimeException("Email already registered");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .mobile(request.getMobile())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .isVerified(false)
                .twoFactorAuthEnabled(false)
                .role(Role.USER)
                .build();

        User savedUser = userRepository.save(user);

        // Every new user gets a wallet with zero balance
        Wallet wallet = Wallet.builder()
                .user(savedUser)
                .balance(BigDecimal.ZERO)
                .build();
        walletRepository.save(wallet);

        String token = jwtUtil.generateToken(savedUser.getEmail());
        return new AuthResponse("Registered successfully", token, false, "SUCCESS");
    }

    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid email or password"));

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        if (user.isTwoFactorAuthEnabled()) {
            otpService.generateAndSendOtp(user, VerificationType.LOGIN_2FA);
            return new AuthResponse("OTP sent to your email for 2FA verification", null, true, "OTP_SENT");
        }

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("Login successful", token, false, "SUCCESS");
    }
    public AuthResponse verifyLoginOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        otpService.verifyOtp(user, VerificationType.LOGIN_2FA, request.getOtp());

        String token = jwtUtil.generateToken(user.getEmail());
        return new AuthResponse("Login successful (2FA verified)", token, false, "SUCCESS");
    }
}