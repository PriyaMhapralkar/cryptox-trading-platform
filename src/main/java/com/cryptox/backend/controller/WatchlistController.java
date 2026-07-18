package com.cryptox.backend.controller;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.UserRepository;
import com.cryptox.backend.service.WatchlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/watchlist")
public class WatchlistController {

    @Autowired private WatchlistService watchlistService;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public Watchlist getWatchlist(Authentication authentication) {
        return watchlistService.getOrCreateWatchlist(currentUser(authentication));
    }

    @PostMapping("/add/{coinId}")
    public Watchlist addCoin(@PathVariable String coinId, Authentication authentication) {
        return watchlistService.addCoin(currentUser(authentication), coinId);
    }

    @DeleteMapping("/remove/{coinId}")
    public Watchlist removeCoin(@PathVariable String coinId, Authentication authentication) {
        return watchlistService.removeCoin(currentUser(authentication), coinId);
    }
}