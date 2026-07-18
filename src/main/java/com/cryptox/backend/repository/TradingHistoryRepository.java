package com.cryptox.backend.repository;

import com.cryptox.backend.entity.TradingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface TradingHistoryRepository extends JpaRepository<TradingHistory, Long> {
    List<TradingHistory> findByUserIdOrderByTimestampDesc(Long userId);
}