package com.am.marketdata.provider.upstox.client;

import com.am.marketdata.provider.upstox.config.UpstoxConfig;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoDetailsWrapperDto;
import com.am.marketdata.provider.upstox.dto.UpstoxIpoListWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Optional;

/**
 * REST Client for communicating directly with Upstox Developer v2 IPO endpoints.
 * Dynamically resolves active access token from Redis session cache ("market_data:upstox:access_token")
 * and spring configuration {@link UpstoxConfig}, following repository design patterns.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpstoxIpoClient {

    private static final String REDIS_KEY_ACCESS_TOKEN = "market_data:upstox:access_token";
    private static final String DEFAULT_BASE_URL = "https://api.upstox.com/v2";

    private final RestTemplate restTemplate;
    private final UpstoxConfig upstoxConfig;
    private final StringRedisTemplate redisTemplate;

    @Value("${upstox.api.baseUrl:https://api.upstox.com/v2}")
    private String configuredBaseUrl;

    /**
     * Resolves active base URL dynamically from UpstoxConfig or property fallback.
     */
    private String getBaseUrl() {
        if (upstoxConfig != null && upstoxConfig.getBaseUrl() != null && !upstoxConfig.getBaseUrl().trim().isEmpty()) {
            return upstoxConfig.getBaseUrl().trim();
        }
        if (configuredBaseUrl != null && !configuredBaseUrl.trim().isEmpty()) {
            return configuredBaseUrl.trim();
        }
        return DEFAULT_BASE_URL;
    }

    /**
     * Resolves active access token dynamically, prioritizing Redis session cache ("market_data:upstox:access_token")
     * first before falling back to static UpstoxConfig.
     */
    private String getAccessToken() {
        try {
            if (redisTemplate != null) {
                String cachedToken = redisTemplate.opsForValue().get(REDIS_KEY_ACCESS_TOKEN);
                if (cachedToken != null && !cachedToken.trim().isEmpty()) {
                    return cachedToken.trim();
                }
            }
        } catch (Exception e) {
            log.warn("Failed to retrieve Upstox access token from Redis: {}", e.getMessage());
        }
        return (upstoxConfig != null) ? upstoxConfig.getAccessToken() : null;
    }

    /**
     * Executes GET /v2/ipos to retrieve listing summaries.
     *
     * @param status lifecycle status filter ("open", "upcoming", "closed", "listed"). Default "open".
     * @param issueType issue segment filter ("regular", "sme").
     * @param pageNumber 1-based page index.
     * @param records items per page (1 to 100).
     * @return UpstoxIpoListWrapperDto payload.
     */
    public UpstoxIpoListWrapperDto fetchIpos(String status, String issueType, Integer pageNumber, Integer records) {
        String effectiveStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toLowerCase() : "open";
        int effectivePage = (pageNumber != null && pageNumber >= 1) ? pageNumber : 1;
        int effectiveRecords = (records != null) ? Math.min(100, Math.max(1, records)) : 30;

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/ipos")
                .queryParam("status", effectiveStatus)
                .queryParam("page_number", effectivePage)
                .queryParam("records", effectiveRecords);

        if (issueType != null && !issueType.trim().isEmpty()) {
            builder.queryParam("issue_type", issueType.trim().toLowerCase());
        }

        URI targetUri = builder.build().encode().toUri();
        log.debug("Executing Upstox GET /v2/ipos query URI: {}", targetUri);

        try {
            ResponseEntity<UpstoxIpoListWrapperDto> response = restTemplate.exchange(
                    targetUri, HttpMethod.GET, createHttpEntity(), UpstoxIpoListWrapperDto.class);
            return response.getBody();
        } catch (HttpStatusCodeException e) {
            log.error("Upstox API call failed for GET /v2/ipos [status={}]: Code {} Payload: {}",
                    effectiveStatus, e.getStatusCode(), e.getResponseBodyAsString());
            UpstoxIpoListWrapperDto fallback = new UpstoxIpoListWrapperDto();
            fallback.setStatus("error");
            fallback.setData(Collections.emptyList());
            return fallback;
        }
    }

    /**
     * Executes GET /v2/ipos/{id} to retrieve 100% full detail for a target IPO.
     *
     * @param ipoId slug identifier string.
     * @return Optional containing UpstoxIpoDetailsWrapperDto if found, or empty.
     */
    public Optional<UpstoxIpoDetailsWrapperDto> fetchIpoDetails(String ipoId) {
        if (ipoId == null || ipoId.trim().isEmpty()) {
            log.warn("Cannot fetch Upstox IPO details: ipoId is blank");
            return Optional.empty();
        }

        String sanitizedId = UriUtils.encodePathSegment(ipoId.trim(), StandardCharsets.UTF_8);
        URI targetUri = UriComponentsBuilder.fromHttpUrl(getBaseUrl() + "/ipos/" + sanitizedId)
                .build(true)
                .toUri();

        log.debug("Executing Upstox GET /v2/ipos/{id} URI: {}", targetUri);

        try {
            ResponseEntity<UpstoxIpoDetailsWrapperDto> response = restTemplate.exchange(
                    targetUri, HttpMethod.GET, createHttpEntity(), UpstoxIpoDetailsWrapperDto.class);
            return Optional.ofNullable(response.getBody());
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Upstox IPO id='{}' not found (HTTP 404)", ipoId);
            return Optional.empty();
        } catch (HttpStatusCodeException e) {
            log.error("Upstox API call failed for GET /v2/ipos/{} : Code {} Payload: {}",
                    ipoId, e.getStatusCode(), e.getResponseBodyAsString());
            return Optional.empty();
        }
    }

    private HttpEntity<Void> createHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("User-Agent", "PostmanRuntime/7.39.0");
        headers.set("Accept-Encoding", "identity");
        String token = getAccessToken();
        if (token != null && !token.trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + token.trim());
        } else {
            log.warn("Executing Upstox API request without Bearer token (token is missing or empty)");
        }
        return new HttpEntity<>(headers);
    }
}
