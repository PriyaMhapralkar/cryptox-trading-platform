package com.cryptox.backend.service;

import com.cryptox.backend.entity.Coin;
import com.cryptox.backend.repository.CoinRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Optional;

@Service
public class ChatService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private CoinRepository coinRepository;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    public String getChatResponse(String userMessage) {
        String context = buildMarketContext(userMessage);
        String finalPrompt = buildPrompt(userMessage, context);

        return callGemini(finalPrompt);
    }

    // Detects if the user mentioned a known coin (by name or symbol) and builds a data snippet
    private String buildMarketContext(String userMessage) {
        String lowerMsg = userMessage.toLowerCase();
        List<Coin> allCoins = coinRepository.findAll();

        Optional<Coin> matched = allCoins.stream()
                .filter(c -> lowerMsg.contains(c.getName().toLowerCase())
                        || lowerMsg.contains(c.getSymbol().toLowerCase()))
                .findFirst();

        if (matched.isEmpty()) {
            return ""; // no specific coin mentioned — let Gemini answer generally
        }

        Coin coin = matched.get();
        return String.format(
                "Live market data for %s (%s) as of now: " +
                "Current price: $%.2f, 24h change: %.2f%%, Market cap: $%.0f, " +
                "Market cap rank: #%d, 24h high: $%.2f, 24h low: $%.2f.",
                coin.getName(), coin.getSymbol().toUpperCase(),
                coin.getCurrentPrice(), coin.getPriceChangePercentage24h(),
                coin.getMarketCap(), coin.getMarketCapRank(),
                coin.getHigh24h(), coin.getLow24h()
        );
    }

    private String buildPrompt(String userMessage, String marketContext) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are CryptoX Assistant, a helpful AI chatbot for a cryptocurrency trading platform. ");
        prompt.append("Answer clearly and concisely. If live market data is provided below, use it as the ");
        prompt.append("authoritative source over your own general knowledge, since it reflects the current moment. ");
        prompt.append("If no market data is provided, answer using your general crypto knowledge, ");
        prompt.append("and mention that you don't have live data for that specific query.\n\n");

        if (!marketContext.isEmpty()) {
            prompt.append("MARKET DATA:\n").append(marketContext).append("\n\n");
        }

        prompt.append("USER QUESTION: ").append(userMessage);

        return prompt.toString();
    }

    private String callGemini(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/"
                + model + ":generateContent?key=" + apiKey;

        JSONObject part = new JSONObject().put("text", prompt);
        JSONObject content = new JSONObject().put("parts", new JSONArray().put(part));
        JSONObject body = new JSONObject().put("contents", new JSONArray().put(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body.toString(), headers);

        try {
            JSONObject response = new JSONObject(
                    restTemplate.postForObject(url, entity, String.class)
            );

            return response
                    .getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");

        } catch (Exception e) {
            return "Sorry, I couldn't process that right now. (" + e.getMessage() + ")";
        }
    }
}