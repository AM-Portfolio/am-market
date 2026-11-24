package com.am.marketdata.service.ipo;

import com.am.marketdata.service.model.IpoGmp;
import com.am.marketdata.service.model.IpoGmpObservation;
import com.am.marketdata.service.model.UpstoxIpoDocument;
import com.am.marketdata.service.repo.UpstoxIpoMongoRepository;
import com.am.marketdata.service.model.IpoSyncMetaDocument;
import com.am.marketdata.service.repo.IpoSyncMetaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Locale;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;

/** Fetches the provider batch once and enriches existing Upstox IPO documents only. */
@Slf4j
@Service
@RequiredArgsConstructor
public class IpoGmpService {
    private final UpstoxIpoMongoRepository repository;
    private final ObjectMapper objectMapper;
    private final IpoSyncMetaRepository metaRepository;

    @Value("${market-data.ipo.gmp.api-key:}") private String apiKey;
    @Value("${market-data.ipo.gmp.base-url:https://www.ipoguru.in/api/v2}") private String baseUrl;

    /**
     * Performs one batch provider request. The job only enriches IPOs already
     * present locally; it never creates an IPO from an external GMP row.
     */
    public IpoGmpRefreshResult refresh() {
        Instant started = Instant.now();
        if (apiKey == null || apiKey.isBlank()) {
            return record(IpoGmpRefreshResult.builder().status("SKIPPED").error("IPOGURU_API_KEY is not configured").build(), started);
        }
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/ipos?months=0"))
                    .header("X-API-KEY", apiKey)
                    .header("Accept", "application/json")
                    .GET().build();
            HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) throw new IllegalStateException("IPO Guru HTTP " + response.statusCode());
            // IPO Guru has returned both a top-level array and wrapped data in
            // different API responses, so accept either shape.
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode rows = root.isArray() ? root : root.path("data");
            if (!rows.isArray()) rows = root.path("ipos");
            if (!rows.isArray()) throw new IllegalStateException("IPO Guru response did not contain an IPO array");
            int received = rows.size(), matched = 0, updated = 0, duplicates = 0, unmatched = 0, skipped = 0;
            String runId = UUID.randomUUID().toString();
            for (JsonNode row : rows) {
                String slug = text(row, "slug", "id");
                String name = normalize(text(row, "name", "company_name", "companyName"));
                if (name.isBlank()) { skipped++; continue; }
                // A normalized name is only a candidate key. Multiple local
                // matches are deliberately skipped instead of risking a wrong GMP.
                List<UpstoxIpoDocument> candidates = repository.findAll().stream()
                        .filter(doc -> normalize(doc.getCompanyName()).equals(name))
                        .collect(Collectors.toList());
                if (candidates.size() != 1) {
                    if (candidates.size() > 1) log.warn("Skipping ambiguous IPO Guru match for '{}': {} local records", name, candidates.size());
                    unmatched++;
                    continue;
                }
                for (UpstoxIpoDocument doc : candidates) {
                    JsonNode g = row.path("gmp");
                    matched++;
                    if (g.isMissingNode() || g.isNull()) { skipped++; continue; }
                    Instant providerUpdated = instant(text(g, "updated_at", "updatedAt"));
                    IpoGmp current = doc.getGmp();
                    if (current != null && current.getUpdatedAt() != null && providerUpdated != null && providerUpdated.isBefore(current.getUpdatedAt())) continue;
                    IpoGmpObservation observation = IpoGmpObservation.builder().provider("IPO_GURU").providerSlug(slug)
                            .price(text(g, "price", "gmp")).percentage(text(g, "percentage"))
                            .kostak(text(g, "kostak")).subjectToSauda(text(g, "subject_to_sauda", "subjectToSauda"))
                            .estimatedListingPrice(text(g, "estimated_listing_price", "estimatedListingPrice"))
                            .updatedAt(providerUpdated).capturedAt(Instant.now(Clock.system(ZoneId.of("Asia/Kolkata")))).captureRunId(runId).build();
                    if (current == null) current = IpoGmp.builder().history(new ArrayList<>()).build();
                    if (current.getHistory() == null) current.setHistory(new ArrayList<>());
                    // Replays of the same provider observation must be idempotent;
                    // a changed provider timestamp/value is a new history point.
                    boolean duplicate = current.getHistory().stream().anyMatch(existing ->
                            same(existing.getProviderSlug(), slug)
                                    && sameInstant(existing.getUpdatedAt(), providerUpdated)
                                    && same(existing.getPrice(), observation.getPrice())
                                    && same(existing.getPercentage(), observation.getPercentage()));
                    if (duplicate) { duplicates++; break; }
                    current.setProvider("IPO_GURU"); current.setProviderSlug(slug); current.setPrice(observation.getPrice());
                    current.setPercentage(observation.getPercentage()); current.setKostak(observation.getKostak());
                    current.setSubjectToSauda(observation.getSubjectToSauda()); current.setEstimatedListingPrice(observation.getEstimatedListingPrice());
                    current.setUpdatedAt(providerUpdated); current.setCapturedAt(observation.getCapturedAt());
                    current.getHistory().add(observation); doc.setGmp(current); doc.setUpdatedAt(Instant.now()); repository.save(doc); updated++;
                    break;
                }
            }
            return record(IpoGmpRefreshResult.builder().status("SUCCESS").received(received).matched(matched)
                    .updated(updated).duplicates(duplicates).unmatched(unmatched).skipped(skipped).build(), started);
        } catch (Exception e) {
            log.error("IPO Guru GMP refresh failed: {}", e.getMessage(), e);
            return record(IpoGmpRefreshResult.builder().status("FAILED").error(e.getMessage()).build(), started);
        }
    }

    private IpoGmpRefreshResult record(IpoGmpRefreshResult result, Instant started) {
        try {
            IpoSyncMetaDocument meta = metaRepository.findById("IPOGURU_GMP").orElseGet(() ->
                    IpoSyncMetaDocument.builder().id("IPOGURU_GMP").source("IPO_GURU").build());
            meta.setLastStartedAt(started); meta.setLastCompletedAt(Instant.now()); meta.setLastStatus(result.getStatus());
            meta.setLastError(result.getError()); meta.setLastCount(result.getUpdated()); meta.setLastTrigger("GMP");
            meta.setReceivedCount(result.getReceived()); meta.setMatchedCount(result.getMatched()); meta.setUpdatedCount(result.getUpdated());
            meta.setDuplicateCount(result.getDuplicates()); meta.setUnmatchedCount(result.getUnmatched()); meta.setSkippedCount(result.getSkipped());
            metaRepository.save(meta);
        } catch (Exception e) { log.warn("Unable to persist IPO Guru GMP run metadata: {}", e.getMessage()); }
        return result;
    }

    private static String text(JsonNode node, String... names) { for (String n : names) if (node.hasNonNull(n)) return node.get(n).asText(); return null; }
    private static String normalize(String value) {
        if (value == null) return "";
        String s = value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
        return s.endsWith("ipo") ? s.substring(0, s.length() - 3) : s;
    }
    private static Instant instant(String value) { try { return value == null ? null : Instant.parse(value); } catch (Exception ignored) { return null; } }
    private static boolean same(String a, String b) { return a == null ? b == null : a.equals(b); }
    private static boolean sameInstant(Instant a, Instant b) { return a == null ? b == null : a.equals(b); }
}
