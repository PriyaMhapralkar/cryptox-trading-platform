package com.cryptox.backend.repository;

import com.cryptox.backend.entity.Withdrawal;
import com.cryptox.backend.entity.WithdrawalStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface WithdrawalRepository extends JpaRepository<Withdrawal, Long> {
    List<Withdrawal> findByUserIdOrderByDateDesc(Long userId);
    List<Withdrawal> findByStatus(WithdrawalStatus status);
}