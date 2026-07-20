package com.cryptox.backend.controller;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.TradingHistoryRepository;
import com.cryptox.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/trading-history")
public class TradingHistoryController {

    @Autowired private TradingHistoryRepository tradingHistoryRepository;
    @Autowired private UserRepository userRepository;

    @GetMapping
    public List<TradingHistory> getMyHistory(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return tradingHistoryRepository.findByUserIdOrderByTimestampDesc(user.getId());
    }
}