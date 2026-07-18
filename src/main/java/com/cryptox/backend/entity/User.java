package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "users")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, unique = true)
    private String email;

    private String mobile;

    @Column(nullable = false)
    @com.fasterxml.jackson.annotation.JsonIgnore
    private String password;

    private String status;

    private boolean isVerified;

    private boolean twoFactorAuthEnabled;

    private String twoFactorAuthSendTo; // EMAIL or MOBILE

    private String picture;

    @Enumerated(EnumType.STRING)
    private Role role; // USER, ADMIN
}