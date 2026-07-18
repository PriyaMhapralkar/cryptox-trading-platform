package com.cryptox.backend.repository;

import com.cryptox.backend.entity.Asset;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface AssetRepository extends JpaRepository<Asset, Long> {
    List<Asset> findByUserId(Long userId);
    Optional<Asset> findByUserIdAndCoinId(Long userId, Long coinId);
}