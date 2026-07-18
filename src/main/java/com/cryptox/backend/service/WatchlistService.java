package com.cryptox.backend.service;

import com.cryptox.backend.entity.*;
import com.cryptox.backend.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WatchlistService {

    @Autowired private WatchlistRepository watchlistRepository;
    @Autowired private CoinRepository coinRepository;

    public Watchlist getOrCreateWatchlist(User user) {
        Watchlist watchlist = watchlistRepository.findByUserId(user.getId());
        if (watchlist == null) {
            watchlist = Watchlist.builder().user(user).build();
            watchlistRepository.save(watchlist);
        }
        return watchlist;
    }

    public Watchlist addCoin(User user, String coinId) {
        Watchlist watchlist = getOrCreateWatchlist(user);
        Coin coin = coinRepository.findByCoinId(coinId)
                .orElseThrow(() -> new RuntimeException("Coin not found: " + coinId));

        watchlist.getCoins().add(coin);
        return watchlistRepository.save(watchlist);
    }

    public Watchlist removeCoin(User user, String coinId) {
        Watchlist watchlist = getOrCreateWatchlist(user);
        Coin coin = coinRepository.findByCoinId(coinId)
                .orElseThrow(() -> new RuntimeException("Coin not found: " + coinId));

        watchlist.getCoins().remove(coin);
        return watchlistRepository.save(watchlist);
    }
}