package com.cryptox.backend.controller;

import com.cryptox.backend.dto.*;
import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wallet")
public class WalletController {

    @Autowired private WalletService walletService;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public Wallet getWallet(Authentication authentication) {
        User user = currentUser(authentication);
        return walletService.getWalletByUserId(user.getId());
    }

    @PutMapping("/add-balance")
    public Wallet addBalance(@RequestBody AddBalanceRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        return walletService.addBalance(user.getId(), request.getAmount());
    }

    @PutMapping("/transfer")
    public String transfer(@RequestBody TransferRequest request, Authentication authentication) {
        User user = currentUser(authentication);
        walletService.transfer(user.getId(), request.getToUserEmail(), request.getAmount(), request.getPurpose());
        return "Transfer successful";
    }

    @GetMapping("/transactions")
    public List<WalletTransaction> getTransactions(Authentication authentication) {
        User user = currentUser(authentication);
        Wallet wallet = walletService.getWalletByUserId(user.getId());
        return walletService.getTransactionHistory(wallet.getId());
    }
}