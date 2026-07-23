package com.cryptox.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class AdminStatsResponse {
    private long totalUsers;
    private long totalTransactions;
    private BigDecimal totalSystemBalance;
    private long pendingWithdrawals;
}