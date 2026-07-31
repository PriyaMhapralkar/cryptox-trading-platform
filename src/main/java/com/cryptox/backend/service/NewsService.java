package com.cryptox.backend.service;

import com.cryptox.backend.dto.NewsItem;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class NewsService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Public RSS feeds — no API key required
    private static final String[] FEEDS = {
            "https://cointelegraph.com/rss",
            "https://decrypt.co/feed"
    };

    public List<NewsItem> getNewsForCoin(String coinName, String symbol) {
        List<NewsItem> matches = new ArrayList<>();

        for (String feedUrl : FEEDS) {
            try {
                String xml = restTemplate.getForObject(feedUrl, String.class);
                if (xml == null) continue;

                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

                NodeList items = doc.getElementsByTagName("item");

                for (int i = 0; i < items.getLength(); i++) {
                    Element item = (Element) items.item(i);
                    String title = getTagValue(item, "title");
                    String link = getTagValue(item, "link");
                    String pubDate = getTagValue(item, "pubDate");

                    if (title == null) continue;

                    String lowerTitle = title.toLowerCase();
                    boolean matchesCoin = lowerTitle.contains(coinName.toLowerCase())
                            || lowerTitle.contains(" " + symbol.toLowerCase() + " ")
                            || lowerTitle.contains(symbol.toLowerCase() + ":");

                    if (matchesCoin) {
                        String source = feedUrl.contains("cointelegraph") ? "Cointelegraph" : "Decrypt";
                        matches.add(new NewsItem(title, link, source, pubDate, 0, 0));
                    }

                    if (matches.size() >= 5) break;
                }
            } catch (Exception e) {
                System.err.println("Failed to fetch/parse feed " + feedUrl + ": " + e.getMessage());
            }

            if (matches.size() >= 5) break;
        }

        return matches;
    }

    private String getTagValue(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        if (nodes.getLength() == 0) return null;
        return nodes.item(0).getTextContent();
    }
}