package com.cryptox.backend.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderRequest {
    private String coinId;     // CoinGecko id, e.g. "bitcoin"
    private String orderType;  // "BUY" or "SELL"
    private BigDecimal amount; // amount in USD the user wants to spend/receive
}