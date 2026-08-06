package com.am.marketdata.service.ipo;

import com.am.marketdata.common.ipo.IpoFeedScope;
import com.am.marketdata.common.ipo.IpoIssue;
import com.am.marketdata.common.ipo.IpoIssueSource;
import com.am.marketdata.common.ipo.IpoLifecycle;
import com.am.marketdata.common.ipo.IpoSubscription;
import com.am.marketdata.common.ipo.IpoSubscriptionCategory;
import com.am.marketdata.service.model.IpoIssueDocument;
import com.am.marketdata.service.model.IpoSubscriptionCategoryEmbedded;
import com.am.marketdata.service.model.IpoSubscriptionEmbedded;
import com.am.marketdata.service.model.IpoSyncMetaDocument;
import com.am.marketdata.service.repo.IpoIssueRepository;
import com.am.marketdata.service.repo.IpoSyncMetaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class IpoSyncService {

    private final Optional<IpoIssueSource> ipoIssueSource;
    private final IpoIssueRepository ipoIssueRepository;
    private final IpoSyncMetaRepository ipoSyncMetaRepository;

    @Value("${market-data.ipo.subscription.max-concurrency:3}")
    private int maxConcurrency;

    @Value("${market-data.ipo.subscription.max-retries:2}")
    private int maxRetries;

    public boolean isSourceAvailable() {
        return ipoIssueSource.isPresent();
    }

    public boolean hasPastData() {
        return ipoIssueRepository.countByLifecycle(IpoLifecycle.PAST.name()) > 0
                || ipoIssueRepository.count() > 0;
    }

    public Optional<IpoSyncMetaDocument> findSyncMeta(IpoFeedScope scope) {
        if (scope == IpoFeedScope.ALL) {
            return Optional.empty();
        }
        return ipoSyncMetaRepository.findById(scope.name());
    }

    public List<IpoSyncMetaDocument> allSyncMeta() {
        return ipoSyncMetaRepository.findAll();
    }

    public int sync(IpoFeedScope scope, IpoSyncTrigger trigger) {
        IpoIssueSource source = ipoIssueSource.orElseThrow(
                () -> new IllegalStateException("No IpoIssueSource bean (market-data.ipo.source)"));
        return switch (scope) {
            case PAST -> syncIssues(IpoFeedScope.PAST, source.fetchPast(), source.sourceId(), trigger);
            case CURRENT -> syncIssues(IpoFeedScope.CURRENT, source.fetchCurrent(), source.sourceId(), trigger);
            case UPCOMING -> syncIssues(IpoFeedScope.UPCOMING, source.fetchUpcoming(), source.sourceId(), trigger);
            case SUBSCRIPTION -> syncSubscriptions(source, trigger);
            case ALL -> {
                int n = 0;
                n += syncIssues(IpoFeedScope.PAST, source.fetchPast(), source.sourceId(), trigger);
                n += syncIssues(IpoFeedScope.CURRENT, source.fetchCurrent(), source.sourceId(), trigger);
                n += syncIssues(IpoFeedScope.UPCOMING, source.fetchUpcoming(), source.sourceId(), trigger);
                n += syncSubscriptions(source, trigger);
                yield n;
            }
        };
    }

    private int syncIssues(
            IpoFeedScope feed, List<IpoIssue> issues, String vendorSource, IpoSyncTrigger trigger) {
        log.info("IPO sync starting feed={} count={} trigger={} source={}",
                feed, issues.size(), trigger, vendorSource);
        try {
            Instant now = Instant.now();
            int saved = 0;
            for (IpoIssue issue : issues) {
                upsertIssue(issue, feed.name(), vendorSource, now);
                saved++;
            }
            saveMeta(feed.name(), now, null, saved, trigger, vendorSource);
            log.info("IPO sync complete feed={} saved={}", feed, saved);
            return saved;
        } catch (RuntimeException e) {
            saveMeta(feed.name(), null, e.getMessage(), null, trigger, vendorSource);
            throw e;
        }
    }

    private int syncSubscriptions(IpoIssueSource source, IpoSyncTrigger trigger) {
        List<IpoIssueDocument> current =
                ipoIssueRepository.findByLifecycleOrderByOpenDateDesc(IpoLifecycle.CURRENT.name());
        if (current.isEmpty()) {
            saveMeta(IpoFeedScope.SUBSCRIPTION.name(), Instant.now(), null, 0, trigger, source.sourceId());
            return 0;
        }
        Semaphore limiter = new Semaphore(Math.max(1, maxConcurrency));
        ExecutorService pool = Executors.newFixedThreadPool(Math.max(1, maxConcurrency));
        try {
            List<CompletableFuture<Boolean>> futures = new ArrayList<>();
            for (IpoIssueDocument doc : current) {
                futures.add(CompletableFuture.supplyAsync(() -> {
                    try {
                        limiter.acquire();
                        return refreshSubscriptionWithRetry(source, doc);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    } finally {
                        limiter.release();
                    }
                }, pool));
            }
            int ok = 0;
            for (CompletableFuture<Boolean> f : futures) {
                if (Boolean.TRUE.equals(f.join())) {
                    ok++;
                }
            }
            saveMeta(IpoFeedScope.SUBSCRIPTION.name(), Instant.now(), null, ok, trigger, source.sourceId());
            return ok;
        } catch (RuntimeException e) {
            saveMeta(IpoFeedScope.SUBSCRIPTION.name(), null, e.getMessage(), null, trigger, source.sourceId());
            throw e;
        } finally {
            pool.shutdown();
            try {
                pool.awaitTermination(2, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private boolean refreshSubscriptionWithRetry(IpoIssueSource source, IpoIssueDocument doc) {
        int attempts = Math.max(1, maxRetries + 1);
        for (int i = 0; i < attempts; i++) {
            try {
                Optional<IpoSubscription> sub =
                        source.fetchSubscription(doc.getSymbol(), doc.getSeries() == null ? "EQ" : doc.getSeries());
                if (sub.isEmpty()) {
                    return false;
                }
                applySubscription(doc, sub.get());
                ipoIssueRepository.save(doc);
                return true;
            } catch (Exception e) {
                if (i == attempts - 1) {
                    log.warn("IPO subscription sync failed for {}: {}", doc.getId(), e.getMessage());
                    return false;
                }
                try {
                    Thread.sleep(250L * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return false;
                }
            }
        }
        return false;
    }

    void upsertIssue(IpoIssue issue, String feed, String vendorSource, Instant now) {
        String id = IpoIssueDocument.idFor(issue.getSymbol(), issue.getOpenDate());
        IpoIssueDocument existing = ipoIssueRepository.findById(id).orElse(null);
        IpoIssueDocument doc = existing == null ? new IpoIssueDocument() : existing;
        if (doc.getId() == null) {
            doc.setId(id);
        }
        doc.setSymbol(prefer(doc.getSymbol(), issue.getSymbol()));
        doc.setSlug(prefer(doc.getSlug(), issue.getSlug()));
        doc.setCompanyName(prefer(doc.getCompanyName(), issue.getCompanyName()));
        doc.setSeries(prefer(doc.getSeries(), issue.getSeries()));
        doc.setLifecycle(mergeLifecycle(doc.getLifecycle(), issue.getLifecycle().name()));
        doc.setStatus(prefer(doc.getStatus(), issue.getStatus().name()));
        doc.setOpenDate(issue.getOpenDate());
        doc.setCloseDate(prefer(doc.getCloseDate(), issue.getCloseDate()));
        doc.setListingDate(prefer(doc.getListingDate(), issue.getListingDate()));
        doc.setLinkRemovalDate(prefer(doc.getLinkRemovalDate(), issue.getLinkRemovalDate()));
        doc.setCurrency(prefer(doc.getCurrency(), issue.getCurrency()));
        doc.setPriceMin(prefer(doc.getPriceMin(), issue.getPriceMin()));
        doc.setPriceMax(prefer(doc.getPriceMax(), issue.getPriceMax()));
        doc.setIssuePrice(prefer(doc.getIssuePrice(), issue.getIssuePrice()));
        doc.setPriceLabel(prefer(doc.getPriceLabel(), issue.getPriceLabel()));
        doc.setSharesOffered(prefer(doc.getSharesOffered(), issue.getSharesOffered()));
        doc.setIssueSizeLabel(prefer(doc.getIssueSizeLabel(), issue.getIssueSizeLabel()));
        doc.setOnNse(doc.isOnNse() || issue.isOnNse());
        doc.setOnBse(doc.isOnBse() || issue.isOnBse());
        doc.setSubscriptionTimes(prefer(doc.getSubscriptionTimes(), issue.getSubscriptionTimes()));
        doc.setSubscriptionSharesBid(prefer(doc.getSubscriptionSharesBid(), issue.getSubscriptionSharesBid()));
        doc.setSubscriptionSharesOffered(
                prefer(doc.getSubscriptionSharesOffered(), issue.getSubscriptionSharesOffered()));
        doc.setVendorSource(vendorSource);
        doc.setLastFeed(feed);
        doc.setSyncedAt(now);
        ipoIssueRepository.save(doc);
    }

    private void applySubscription(IpoIssueDocument doc, IpoSubscription sub) {
        IpoSubscriptionEmbedded embedded = new IpoSubscriptionEmbedded();
        embedded.setSeries(sub.getSeries());
        embedded.setUpdatedAt(sub.getUpdatedAt());
        embedded.setOverallTimes(sub.getOverallTimes());
        embedded.setOverallSharesBid(sub.getOverallSharesBid());
        embedded.setOverallSharesOffered(sub.getOverallSharesOffered());
        embedded.setCategories(mapCategories(sub.getCategories()));
        embedded.setSyncedAt(Instant.now());
        doc.setSubscription(embedded);
        doc.setSubscriptionTimes(prefer(doc.getSubscriptionTimes(), sub.getOverallTimes()));
        doc.setSubscriptionSharesBid(prefer(doc.getSubscriptionSharesBid(), sub.getOverallSharesBid()));
        doc.setSubscriptionSharesOffered(
                prefer(doc.getSubscriptionSharesOffered(), sub.getOverallSharesOffered()));
        doc.setSyncedAt(Instant.now());
    }

    private static List<IpoSubscriptionCategoryEmbedded> mapCategories(List<IpoSubscriptionCategory> cats) {
        List<IpoSubscriptionCategoryEmbedded> out = new ArrayList<>();
        if (cats == null) {
            return out;
        }
        for (IpoSubscriptionCategory cat : cats) {
            out.add(IpoSubscriptionCategoryEmbedded.builder()
                    .code(cat.getCode())
                    .name(cat.getName())
                    .srNo(cat.getSrNo())
                    .sharesOffered(cat.getSharesOffered())
                    .sharesBid(cat.getSharesBid())
                    .times(cat.getTimes())
                    .children(mapCategories(cat.getChildren()))
                    .build());
        }
        return out;
    }

    static String mergeLifecycle(String existing, String incoming) {
        if (existing == null || existing.isBlank()) {
            return incoming;
        }
        int e = rank(existing);
        int i = rank(incoming);
        return i >= e ? incoming : existing;
    }

    private static int rank(String lifecycle) {
        return switch (lifecycle) {
            case "UPCOMING" -> 1;
            case "CURRENT" -> 2;
            case "PAST" -> 3;
            default -> 0;
        };
    }

    private static <T> T prefer(T existing, T incoming) {
        return incoming != null ? incoming : existing;
    }

    private void saveMeta(
            String id,
            Instant syncedAt,
            String error,
            Integer count,
            IpoSyncTrigger trigger,
            String source) {
        IpoSyncMetaDocument existing = ipoSyncMetaRepository.findById(id).orElse(null);
        IpoSyncMetaDocument.IpoSyncMetaDocumentBuilder b = IpoSyncMetaDocument.builder()
                .id(id)
                .lastError(error)
                .lastCount(count)
                .lastTrigger(trigger.name())
                .source(source);
        if (syncedAt != null) {
            b.lastFullSyncAt(syncedAt);
        } else if (existing != null) {
            b.lastFullSyncAt(existing.getLastFullSyncAt());
        }
        ipoSyncMetaRepository.save(b.build());
    }
}
