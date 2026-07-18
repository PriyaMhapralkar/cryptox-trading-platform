package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "verification_codes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VerificationCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String otp;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String email;
    private String mobile;

    @Enumerated(EnumType.STRING)
    private VerificationType verificationType;

    private LocalDateTime expiresAt;
    private boolean isUsed;
}