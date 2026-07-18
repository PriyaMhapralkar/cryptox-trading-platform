package com.cryptox.backend.repository;

import com.cryptox.backend.entity.Coin;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CoinRepository extends JpaRepository<Coin, Long> {
    Optional<Coin> findByCoinId(String coinId);
}