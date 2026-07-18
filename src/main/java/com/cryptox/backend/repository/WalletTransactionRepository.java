package com.cryptox.backend.repository;

import com.cryptox.backend.entity.WalletTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, Long> {
    List<WalletTransaction> findByWalletIdOrderByDateDesc(Long walletId);
}