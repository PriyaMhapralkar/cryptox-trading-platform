package com.cryptox.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class NewsItem {
    private String title;
    private String url;
    private String source;
    private String publishedAt;
    private int positiveVotes;
    private int negativeVotes;
}