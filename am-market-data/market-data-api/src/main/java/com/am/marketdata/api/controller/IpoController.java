package com.am.marketdata.api.controller;

import com.am.marketdata.service.ipo.IpoQueryService;
import com.am.marketdata.service.model.IpoIssueDocument;
import com.am.marketdata.service.model.IpoSubscriptionCategoryEmbedded;
import com.am.marketdata.service.model.IpoSubscriptionEmbedded;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/v1/ipo")
@RequiredArgsConstructor
public class IpoController {

    private final IpoQueryService ipoQueryService;

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String series,
            @RequestParam(required = false) String q) {
        List<IpoIssueDocument> docs = ipoQueryService.list(lifecycle, series, q);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", docs.stream().map(this::toListItem).collect(Collectors.toList()));
        body.put("count", docs.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{symbol}")
    public ResponseEntity<Map<String, Object>> bySymbol(@PathVariable String symbol) {
        List<IpoIssueDocument> docs = ipoQueryService.bySymbol(symbol);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("data", docs.stream().map(this::toDetail).collect(Collectors.toList()));
        body.put("count", docs.size());
        return ResponseEntity.ok(body);
    }

    @GetMapping("/{symbol}/{openDate}")
    public ResponseEntity<Map<String, Object>> detail(
            @PathVariable String symbol,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate) {
        return ipoQueryService
                .bySymbolAndOpenDate(symbol, openDate)
                .map(doc -> ResponseEntity.ok(toDetail(doc)))
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{symbol}/{openDate}/subscription")
    public ResponseEntity<Map<String, Object>> subscription(
            @PathVariable String symbol,
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate openDate) {
        return ipoQueryService
                .bySymbolAndOpenDate(symbol, openDate)
                .map(doc -> {
                    if (doc.getSubscription() == null) {
                        return ResponseEntity.notFound().<Map<String, Object>>build();
                    }
                    Map<String, Object> body = toSubscription(doc.getId(), doc.getSymbol(), doc.getSubscription());
                    return ResponseEntity.ok(body);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private Map<String, Object> toListItem(IpoIssueDocument doc) {
        Map<String, Object> m = baseIssue(doc);
        m.put("subscriptionSummary", subscriptionSummary(doc));
        return m;
    }

    private Map<String, Object> toDetail(IpoIssueDocument doc) {
        Map<String, Object> m = baseIssue(doc);
        m.put("subscriptionSummary", subscriptionSummary(doc));
        if (doc.getSubscription() != null) {
            m.put("subscription", toSubscription(doc.getId(), doc.getSymbol(), doc.getSubscription()));
        }
        return m;
    }

    private Map<String, Object> baseIssue(IpoIssueDocument doc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", doc.getId());
        m.put("symbol", doc.getSymbol());
        m.put("slug", doc.getSlug());
        m.put("companyName", doc.getCompanyName());
        m.put("series", doc.getSeries());
        m.put("lifecycle", doc.getLifecycle());
        m.put("status", doc.getStatus());
        Map<String, Object> window = new LinkedHashMap<>();
        window.put("openDate", doc.getOpenDate());
        window.put("closeDate", doc.getCloseDate());
        window.put("listingDate", doc.getListingDate());
        window.put("linkRemovalDate", doc.getLinkRemovalDate());
        m.put("window", window);
        Map<String, Object> pricing = new LinkedHashMap<>();
        pricing.put("currency", doc.getCurrency());
        pricing.put("priceMin", doc.getPriceMin());
        pricing.put("priceMax", doc.getPriceMax());
        pricing.put("issuePrice", doc.getIssuePrice());
        pricing.put("label", doc.getPriceLabel());
        m.put("pricing", pricing);
        Map<String, Object> size = new LinkedHashMap<>();
        size.put("sharesOffered", doc.getSharesOffered());
        size.put("issueSizeLabel", doc.getIssueSizeLabel());
        m.put("size", size);
        Map<String, Object> exchanges = new LinkedHashMap<>();
        exchanges.put("nse", doc.isOnNse());
        exchanges.put("bse", doc.isOnBse());
        m.put("exchanges", exchanges);
        Map<String, Object> vendor = new LinkedHashMap<>();
        vendor.put("source", doc.getVendorSource());
        m.put("vendor", vendor);
        m.put("syncedAt", doc.getSyncedAt());
        return m;
    }

    private Map<String, Object> subscriptionSummary(IpoIssueDocument doc) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("times", doc.getSubscriptionTimes());
        s.put("sharesBid", doc.getSubscriptionSharesBid());
        s.put("sharesOffered", doc.getSubscriptionSharesOffered());
        if (doc.getSubscription() != null) {
            s.put("updatedAt", doc.getSubscription().getUpdatedAt());
        }
        return s;
    }

    private Map<String, Object> toSubscription(
            String issueId, String symbol, IpoSubscriptionEmbedded sub) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("issueId", issueId);
        m.put("symbol", symbol);
        m.put("series", sub.getSeries());
        m.put("updatedAt", sub.getUpdatedAt());
        Map<String, Object> overall = new LinkedHashMap<>();
        overall.put("times", sub.getOverallTimes());
        overall.put("sharesBid", sub.getOverallSharesBid());
        overall.put("sharesOffered", sub.getOverallSharesOffered());
        m.put("overall", overall);
        m.put("categories", mapCats(sub.getCategories()));
        return m;
    }

    private List<Map<String, Object>> mapCats(List<IpoSubscriptionCategoryEmbedded> cats) {
        List<Map<String, Object>> out = new ArrayList<>();
        if (cats == null) {
            return out;
        }
        for (IpoSubscriptionCategoryEmbedded cat : cats) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("code", cat.getCode());
            m.put("name", cat.getName());
            m.put("srNo", cat.getSrNo());
            m.put("sharesOffered", cat.getSharesOffered());
            m.put("sharesBid", cat.getSharesBid());
            m.put("times", cat.getTimes());
            m.put("children", mapCats(cat.getChildren()));
            out.add(m);
        }
        return out;
    }
}
