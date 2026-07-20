package com.cryptox.backend.controller;

import com.cryptox.backend.entity.Coin;
import com.cryptox.backend.service.CoinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/coins")
public class CoinController {

    @Autowired
    private CoinService coinService;

    @GetMapping
    public List<Coin> getCoins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "all") String category) {

        return switch (category.toLowerCase()) {
            case "top50" -> coinService.getTop50();
            case "gainers" -> coinService.getTopGainers();
            case "losers" -> coinService.getTopLosers();
            default -> coinService.getAllCoins(page, size);
        };
    }

    @GetMapping("/search")
    public List<Coin> search(@RequestParam String q) {
        return coinService.searchCoins(q);
    }

    @GetMapping("/{coinId}")
    public Coin getCoin(@PathVariable String coinId) {
        return coinService.getCoinById(coinId);
    }

    @GetMapping("/{coinId}/chart")
    public Map<String, Object> getChart(
            @PathVariable String coinId,
            @RequestParam(defaultValue = "1") String days) {
        return coinService.getCoinChart(coinId, days);
    }
    @GetMapping("/count")
    public long getCoinCount() {
        return coinService.getTotalCoinCount();
    }
}