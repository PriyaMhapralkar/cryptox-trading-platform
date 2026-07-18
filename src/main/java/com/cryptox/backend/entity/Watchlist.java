package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.*;

@Entity
@Table(name = "watchlists")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class Watchlist {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToMany
    @JoinTable(
        name = "watchlist_coins",
        joinColumns = @JoinColumn(name = "watchlist_id"),
        inverseJoinColumns = @JoinColumn(name = "coin_id")
    )
    @Builder.Default
    private Set<Coin> coins = new HashSet<>();
}