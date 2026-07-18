package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    private OrderType orderType; // BUY, SELL

    private BigDecimal price;
    private LocalDateTime timestamp;

    @Enumerated(EnumType.STRING)
    private OrderStatus status; // PENDING, SUCCESS, FAILED

    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "order_item_id")
    private OrderItem orderItem;
}