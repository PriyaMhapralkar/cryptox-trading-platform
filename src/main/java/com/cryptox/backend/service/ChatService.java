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
                .filter(c -> {
                    String name = c.getName().toLowerCase();
                    String symbol = c.getSymbol().toLowerCase();

                    boolean nameMatch = containsWholeWord(lowerMsg, name);
                    // Skip symbol matching entirely for very short/common symbols (1-2 chars)
                    // to avoid false positives like "m" matching "market"
                    boolean symbolMatch = symbol.length() >= 3 && containsWholeWord(lowerMsg, symbol);

                    return nameMatch || symbolMatch;
                })
                .findFirst();

        if (matched.isEmpty()) {
            return "";
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

    private boolean containsWholeWord(String text, String word) {
        // \b = word boundary, so "eth" won't match inside "method" or "wealth"
        return java.util.regex.Pattern.compile("\\b" + java.util.regex.Pattern.quote(word) + "\\b")
                .matcher(text)
                .find();
    }
    

    private String buildPrompt(String userMessage, String marketContext) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are "CryptoX Assistant", an intelligent AI assistant integrated into a cryptocurrency trading platform. Your goal is to answer ANY user question in a helpful, accurate, and safe way.

            -----------------------------------
            CORE BEHAVIOR RULES
            -----------------------------------
            1. UNIVERSAL QUESTION HANDLING
            - You must answer all types of questions: crypto-related, platform-related (CryptoX features), general knowledge, casual or unclear questions.
            - Never ignore a question unless it violates safety rules.

            2. DATA PRIORITY (VERY IMPORTANT)
            - If the question is about coin prices, gainers/losers, trending coins, or user portfolio → ALWAYS prioritize the live data provided below over your own general knowledge.
            - If real data is NOT available for the specific thing asked: clearly say "I don't have that data right now."
            - NEVER generate fake prices or fake market data.

            3. NO HALLUCINATION POLICY
            - Do NOT guess unknown facts. Do NOT invent numbers, prices, or statistics.
            - If unsure, say "I'm not sure about that" or "I don't have enough data."

            4. FINANCIAL SAFETY
            - Do NOT give guaranteed profit advice or statements like "this coin will go up."
            - Instead say markets are volatile and encourage the user to do their own research (DYOR).

            5. SMART RESPONSE STYLE
            - Be clear, concise, and helpful. Use simple language for beginners.
            - Maintain a friendly, professional, slightly conversational tone. Not robotic, not overly long.

            6. PLATFORM AWARENESS
            - If asked about CryptoX itself (e.g. "How do I buy crypto?"), explain using the platform's actual flow: go to a coin's page, choose Buy or Sell, enter the USD amount, confirm — funds come from the user's CryptoX wallet.
            - Other platform features: Wallet (add balance via Razorpay, transfer to other users, withdraw to bank), Watchlist (save coins to track), Portfolio (view holdings and P&L), Activity (trading history).

            7. FALLBACK HANDLING
            - If a question is unclear, ask a short clarifying question instead of guessing.

            8. SECURITY & PRIVACY
            - Never expose other users' data or sensitive account info. If asked, politely refuse.

            9. ANALYSIS QUESTIONS
            - Provide general market insights, not predictions (e.g. "movements like this are often tied to broader sentiment, BTC dominance, or macro news" — not "it will keep going up").

            Priority order when these principles conflict: Accuracy > Helpfulness > Safety > Clarity.
            """);

        if (!marketContext.isEmpty()) {
            prompt.append("\nLIVE PLATFORM DATA (treat as authoritative and current):\n")
                  .append(marketContext)
                  .append("\n");
        } else {
            prompt.append("\nNote: No specific live coin/market data was matched for this question. " +
                    "If the user is asking about a specific price, gainer/loser list, or trending coins " +
                    "and none was found above, say so plainly rather than guessing.\n");
        }

        prompt.append("\nUSER QUESTION: ").append(userMessage);

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