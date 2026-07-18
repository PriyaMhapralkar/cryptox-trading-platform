package com.cryptox.backend.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "coins")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Coin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(unique = true)
    private String coinId;

    private String symbol;
    private String name;
    private String image;

    private Double currentPrice;
    private Double marketCap;
    private Integer marketCapRank;
    private Double fullyDilutedValuation;
    private Double totalVolume;
    private Double high24h;
    private Double low24h;
    private Double priceChange24h;
    private Double priceChangePercentage24h;
    private Double marketCapChange24h;
    private Double marketCapChangePercentage24h;
    private Double circulatingSupply;
    private Double totalSupply;
    private Double maxSupply;
    private Double ath;
    private Double athChangePercentage;
    private LocalDateTime athDate;
    private Double atl;
    private Double atlChangePercentage;
    private LocalDateTime atlDate;

    private LocalDateTime lastUpdated;
}