package com.cryptox.backend.controller;

import com.cryptox.backend.dto.WithdrawalRequest;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.service.WithdrawalService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class WithdrawalController {

    @Autowired private WithdrawalService withdrawalService;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // ---- User-facing ----

    @PostMapping("/withdrawal")
    public Withdrawal requestWithdrawal(@RequestBody WithdrawalRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return withdrawalService.requestWithdrawal(user, request.getAmount());
    }

    @GetMapping("/withdrawal/history")
    public List<Withdrawal> getMyWithdrawals(Authentication authentication) {
        User user = currentUser(authentication);
        return withdrawalService.getUserWithdrawals(user.getId());
    }

    // ---- Admin-only ----

    @GetMapping("/admin/withdrawal")
    public List<Withdrawal> getAllWithdrawals(Authentication authentication) {
        requireAdmin(authentication);
        return withdrawalService.getAllWithdrawals();
    }

    @PutMapping("/admin/withdrawal/{id}/proceed")
    public Withdrawal proceedWithdrawal(
            @PathVariable Long id,
            @RequestParam boolean approve,
            Authentication authentication) {
        requireAdmin(authentication);
        return withdrawalService.processWithdrawal(id, approve);
    }

    private void requireAdmin(Authentication authentication) {
        User user = currentUser(authentication);
        if (user.getRole() != Role.ADMIN) {
            throw new RuntimeException("Access denied: admin only");
        }
    }
}