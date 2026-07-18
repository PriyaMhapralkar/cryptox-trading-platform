package com.cryptox.backend.controller;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.AssetRepository;
import com.cryptox.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/assets")
public class AssetController {

    @Autowired private AssetRepository assetRepository;
    @Autowired private UserRepository userRepository;

    private User currentUser(Authentication authentication) {
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    @GetMapping
    public List<Asset> getMyAssets(Authentication authentication) {
        User user = currentUser(authentication);
        return assetRepository.findByUserId(user.getId());
    }

    @GetMapping("/{assetId}")
    public Asset getAsset(@PathVariable Long assetId) {
        return assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
    }
}