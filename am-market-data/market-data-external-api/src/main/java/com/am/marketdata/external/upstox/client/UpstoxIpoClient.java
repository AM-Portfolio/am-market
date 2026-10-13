package com.am.marketdata.external.upstox.client;

import com.am.marketdata.external.upstox.dto.UpstoxIpoDetailsWrapperDto;
import com.am.marketdata.external.upstox.dto.UpstoxIpoListWrapperDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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
 * Handles HTTP headers, UriComponentsBuilder request formatting, path encoding, and error handling.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UpstoxIpoClient {

    private final RestTemplate restTemplate;

    @Value("${upstox.api.baseUrl:https://api.upstox.com/v2}")
    private String baseUrl;

    @Value("${upstox.api.accessToken:}")
    private String accessToken;

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

        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ipos")
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
        URI targetUri = UriComponentsBuilder.fromHttpUrl(baseUrl + "/ipos/" + sanitizedId)
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
        if (accessToken != null && !accessToken.trim().isEmpty()) {
            headers.set("Authorization", "Bearer " + accessToken.trim());
        }
        return new HttpEntity<>(headers);
    }
}
