package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_orders")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    private PaymentOrderStatus status; // CREATED, SUCCESS, FAILED

    @Enumerated(EnumType.STRING)
    private PaymentMethod paymentMethod; // RAZORPAY, STRIPE

    private String gatewayOrderId;
    private String gatewayPaymentId;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime createdAt;
}