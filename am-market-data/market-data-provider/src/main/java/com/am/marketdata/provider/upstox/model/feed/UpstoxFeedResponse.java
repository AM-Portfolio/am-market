package com.am.marketdata.provider.upstox.model.feed;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UpstoxFeedResponse {
    private String type;
    private Map<String, FeedItem> feeds;
    private String currentTs;
}
