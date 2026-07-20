package com.cryptox.backend.controller;

import com.cryptox.backend.entity.User;
import com.cryptox.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @PostMapping("/enable-2fa")
    public String enable2fa(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorAuthEnabled(true);
        userRepository.save(user);
        return "Two-factor authentication enabled";
    }
    
    @GetMapping("/profile")
    public User getProfile(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @PostMapping("/disable-2fa")
    public String disable2fa(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setTwoFactorAuthEnabled(false);
        userRepository.save(user);
        return "Two-factor authentication disabled";
    }
}