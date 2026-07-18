package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "payment_details")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PaymentDetails {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String accountNumber;
    private String accountHolderName;
    private String ifsc;
    private String bankName;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
}