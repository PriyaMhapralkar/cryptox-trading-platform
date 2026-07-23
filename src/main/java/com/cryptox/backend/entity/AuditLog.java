package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    private String action; // LOGIN, TRANSFER, WITHDRAWAL, BUY, SELL, REGISTER

    private String status; // SUCCESS, FAILED

    private String details;

    private LocalDateTime timestamp;
}