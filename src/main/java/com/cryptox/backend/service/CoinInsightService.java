package com.cryptox.backend.service;

import com.cryptox.backend.dto.NewsItem;
import com.cryptox.backend.entity.Coin;
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

@Service
public class CoinInsightService {

    @Autowired private RestTemplate restTemplate;
    @Autowired private NewsService newsService;

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.model}")
    private String model;

    public String generateInsight(Coin coin, List<NewsItem> news) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are a crypto market analyst. Based ONLY on the data below, write a short, ")
              .append("2-3 sentence explanation of likely factors behind this coin's recent price movement. ")
              .append("Do not predict future price direction. Do not give financial advice. ")
              .append("If the news doesn't clearly explain the move, say the move looks driven by ")
              .append("general market sentiment rather than inventing a specific cause.\n\n");

        prompt.append(String.format(
                "COIN: %s (%s)\n24h price change: %.2f%%\n24h trading volume: $%.0f\n\n",
                coin.getName(), coin.getSymbol().toUpperCase(),
                coin.getPriceChangePercentage24h(), coin.getTotalVolume()
        ));

        if (news.isEmpty()) {
            prompt.append("No recent news headlines were found for this coin.\n");
        } else {
            prompt.append("Recent headlines:\n");
            for (NewsItem item : news) {
                prompt.append("- ").append(item.getTitle()).append("\n");
            }
        }

        return callGemini(prompt.toString());
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
            JSONObject response = new JSONObject(restTemplate.postForObject(url, entity, String.class));
            return response.getJSONArray("candidates")
                    .getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text");
        } catch (Exception e) {
            return "Unable to generate insight right now.";
        }
    }
}