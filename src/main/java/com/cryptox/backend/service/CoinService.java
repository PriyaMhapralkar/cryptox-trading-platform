package com.cryptox.backend.service;

import com.cryptox.backend.dto.CoinGeckoMarketDto;
import com.cryptox.backend.entity.Coin;
import com.cryptox.backend.repository.CoinRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class CoinService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CoinRepository coinRepository;

    @Value("${coingecko.api.baseurl}")
    private String baseUrl;

    @Value("${coingecko.api.key}")
    private String apiKey;

    // Runs on startup, then every 3 minutes — respects the free 10,000 calls/month cap
    @Scheduled(fixedRate = 180000, initialDelay = 0)
    public void syncCoinsFromCoinGecko() {
        try {
            String url = baseUrl + "/coins/markets?vs_currency=usd&order=market_cap_desc&per_page=100&page=1&sparkline=false";

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-cg-demo-api-key", apiKey);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<CoinGeckoMarketDto[]> response = restTemplate.exchange(
                    url, HttpMethod.GET, entity, CoinGeckoMarketDto[].class);

            CoinGeckoMarketDto[] coins = response.getBody();
            if (coins == null) return;

            for (CoinGeckoMarketDto dto : coins) {
                Coin coin = coinRepository.findByCoinId(dto.getId()).orElse(new Coin());
                mapDtoToEntity(dto, coin);
                coinRepository.save(coin);
            }

            System.out.println("CoinGecko sync completed: " + coins.length + " coins updated.");
        } catch (Exception e) {
            System.err.println("CoinGecko sync failed: " + e.getMessage());
        }
    }

    private void mapDtoToEntity(CoinGeckoMarketDto dto, Coin coin) {
        coin.setCoinId(dto.getId());
        coin.setSymbol(dto.getSymbol());
        coin.setName(dto.getName());
        coin.setImage(dto.getImage());
        coin.setCurrentPrice(dto.getCurrent_price());
        coin.setMarketCap(dto.getMarket_cap());
        coin.setMarketCapRank(dto.getMarket_cap_rank());
        coin.setFullyDilutedValuation(dto.getFully_diluted_valuation());
        coin.setTotalVolume(dto.getTotal_volume());
        coin.setHigh24h(dto.getHigh_24h());
        coin.setLow24h(dto.getLow_24h());
        coin.setPriceChange24h(dto.getPrice_change_24h());
        coin.setPriceChangePercentage24h(dto.getPrice_change_percentage_24h());
        coin.setMarketCapChange24h(dto.getMarket_cap_change_24h());
        coin.setMarketCapChangePercentage24h(dto.getMarket_cap_change_percentage_24h());
        coin.setCirculatingSupply(dto.getCirculating_supply());
        coin.setTotalSupply(dto.getTotal_supply());
        coin.setMaxSupply(dto.getMax_supply());
        coin.setAth(dto.getAth());
        coin.setAthChangePercentage(dto.getAth_change_percentage());
        coin.setAthDate(parseDate(dto.getAth_date()));
        coin.setAtl(dto.getAtl());
        coin.setAtlChangePercentage(dto.getAtl_change_percentage());
        coin.setAtlDate(parseDate(dto.getAtl_date()));
        coin.setLastUpdated(parseDate(dto.getLast_updated()));
    }

    private LocalDateTime parseDate(String isoDate) {
        if (isoDate == null) return null;
        return OffsetDateTime.parse(isoDate, DateTimeFormatter.ISO_DATE_TIME).toLocalDateTime();
    }

    // ----- Query methods used by the controller -----

    public List<Coin> getAllCoins(int page, int size) {
        List<Coin> all = coinRepository.findAll(
                org.springframework.data.domain.PageRequest.of(page, size,
                        org.springframework.data.domain.Sort.by("marketCapRank").ascending())
        ).getContent();
        return all;
    }

    public List<Coin> getTop50() {
        return coinRepository.findAll(
                org.springframework.data.domain.PageRequest.of(0, 50,
                        org.springframework.data.domain.Sort.by("marketCapRank").ascending())
        ).getContent();
    }

    public List<Coin> getTopGainers() {
        List<Coin> all = coinRepository.findAll();
        all.sort(Comparator.comparing(Coin::getPriceChangePercentage24h,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return all.subList(0, Math.min(50, all.size()));
    }

    public List<Coin> getTopLosers() {
        List<Coin> all = coinRepository.findAll();
        all.sort(Comparator.comparing(Coin::getPriceChangePercentage24h,
                Comparator.nullsLast(Comparator.naturalOrder())));
        return all.subList(0, Math.min(50, all.size()));
    }

    public Coin getCoinById(String coinId) {
        return coinRepository.findByCoinId(coinId)
                .orElseThrow(() -> new RuntimeException("Coin not found: " + coinId));
    }

    public List<Coin> searchCoins(String query) {
        return coinRepository.findAll().stream()
                .filter(c -> c.getName().toLowerCase().contains(query.toLowerCase())
                        || c.getSymbol().toLowerCase().contains(query.toLowerCase()))
                .toList();
    }

    // ----- Chart data (fetched live, not stored) -----

    public Map<String, Object> getCoinChart(String coinId, String days) {
        String url = baseUrl + "/coins/" + coinId + "/market_chart?vs_currency=usd&days=" + days;

        HttpHeaders headers = new HttpHeaders();
        headers.set("x-cg-demo-api-key", apiKey);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        ResponseEntity<Map> response = restTemplate.exchange(url, HttpMethod.GET, entity, Map.class);
        return response.getBody();
    }
}