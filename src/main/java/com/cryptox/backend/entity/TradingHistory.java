package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trading_histories")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class TradingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal sellingPrice;
    private BigDecimal buyingPrice;
    private BigDecimal quantity;
    private BigDecimal profitLoss;

    @ManyToOne
    @JoinColumn(name = "coin_id", nullable = false)
    private Coin coin;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private OrderType orderType; // BUY or SELL, for the Activity page column
}