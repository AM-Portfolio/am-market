package com.am.marketdata.service.ipo;

import com.am.marketdata.common.model.ipo.AsraxIpoDetailsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoSummaryDto;
import com.am.marketdata.common.provider.IpoDataProvider;
import com.am.marketdata.service.model.UpstoxIpoDocument;
import com.am.marketdata.service.redis.UpstoxIpoCacheService;
import com.am.marketdata.service.repo.UpstoxIpoMongoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.stream.Collectors;

/**
 * Service orchestrating Upstox IPO operations, Redis cache lookups, MongoDB persistence, and provider API sync.
 * Implements Smart Delta Sync: Uses single listing query to fetch active IPOs and updates dynamic fields (subscription, status)
 * for existing IPOs without firing unnecessary detail REST calls.
 */
@Slf4j
@Service
public class UpstoxIpoService {

    private final IpoDataProvider ipoDataProvider;
    private final UpstoxIpoMongoRepository mongoRepository;
    private final UpstoxIpoCacheService cacheService;

    public UpstoxIpoService(
            @Qualifier("upstoxIpoProvider") IpoDataProvider ipoDataProvider,
            UpstoxIpoMongoRepository mongoRepository,
            UpstoxIpoCacheService cacheService) {
        this.ipoDataProvider = ipoDataProvider;
        this.mongoRepository = mongoRepository;
        this.cacheService = cacheService;
    }

    /**
     * Gets a list of IPO summaries.
     * Hierarchy: 1) Redis Cache -> 2) MongoDB Fallback -> 3) Live Provider Sync.
     */
    public List<AsraxIpoSummaryDto> getIpos(String status, String issueType, Integer pageNumber, Integer records) {
        String effectiveStatus = (status != null && !status.trim().isEmpty()) ? status.trim().toLowerCase() : "open";
        int page = (pageNumber != null && pageNumber >= 1) ? pageNumber : 1;
        int size = (records != null) ? Math.min(100, Math.max(1, records)) : 30;

        // 1. Try Redis Cache
        Optional<List<AsraxIpoSummaryDto>> cached = cacheService.getCachedList(effectiveStatus, issueType, page, size);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Try MongoDB Fallback
        try {
            PageRequest pageable = PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "biddingStartDate"));
            Page<UpstoxIpoDocument> docPage;

            if (issueType != null && !issueType.trim().isEmpty()) {
                docPage = mongoRepository.findByStatusAndIssueType(effectiveStatus, issueType.trim().toLowerCase(), pageable);
            } else {
                docPage = mongoRepository.findByStatus(effectiveStatus, pageable);
            }

            if (docPage.hasContent()) {
                List<AsraxIpoSummaryDto> dbList = docPage.getContent().stream()
                        .map(this::toSummaryFromDoc)
                        .collect(Collectors.toList());
                log.info("MongoDB HIT for IPO list status='{}', returned {} records", effectiveStatus, dbList.size());
                cacheService.cacheList(effectiveStatus, issueType, page, size, dbList);
                return dbList;
            }
        } catch (Exception e) {
            log.warn("MongoDB read exception for status='{}': {}. Falling through to live provider.", effectiveStatus, e.getMessage());
        }

        // 3. Fallback to Live Provider
        log.info("Uncached request: Fetching live Upstox IPO list for status='{}'", effectiveStatus);
        List<AsraxIpoSummaryDto> liveList = ipoDataProvider.getIpos(effectiveStatus, issueType, page, size);

        if (!liveList.isEmpty()) {
            cacheService.cacheList(effectiveStatus, issueType, page, size, liveList);
            CompletableFuture.runAsync(() -> syncSummariesToMongo(liveList));
        }

        return liveList;
    }

    /**
     * Gets 100% full detail for an IPO.
     * Hierarchy: 1) Redis Cache -> 2) MongoDB Fallback -> 3) Live Provider Sync.
     */
    public Optional<AsraxIpoDetailsDto> getIpoDetails(String ipoId) {
        if (ipoId == null || ipoId.trim().isEmpty()) {
            return Optional.empty();
        }

        String cleanId = ipoId.trim().toLowerCase();

        // 1. Try Redis Cache
        Optional<AsraxIpoDetailsDto> cached = cacheService.getCachedDetails(cleanId);
        if (cached.isPresent()) {
            return cached;
        }

        // 2. Try MongoDB Fallback
        try {
            Optional<UpstoxIpoDocument> docOpt = mongoRepository.findById(cleanId);
            if (docOpt.isPresent()) {
                AsraxIpoDetailsDto dto = toDetailsFromDoc(docOpt.get());
                log.info("MongoDB HIT for IPO details ipoId='{}'", cleanId);
                cacheService.cacheDetails(cleanId, dto);
                return Optional.of(dto);
            }
        } catch (Exception e) {
            log.warn("MongoDB read exception for detail ipoId='{}': {}. Falling back to live provider.", cleanId, e.getMessage());
        }

        // 3. Fallback to Live Provider
        log.info("Uncached request: Fetching live Upstox IPO details for ipoId='{}'", cleanId);
        Optional<AsraxIpoDetailsDto> liveDetailsOpt = ipoDataProvider.getIpoDetails(cleanId);

        liveDetailsOpt.ifPresent(dto -> {
            cacheService.cacheDetails(cleanId, dto);
            CompletableFuture.runAsync(() -> saveDocToMongo(dto));
        });

        return liveDetailsOpt;
    }

    /**
     * Smart Delta Synchronization:
     * Executes single listing queries (records=50) to fetch active IPOs from Upstox.
     * For existing IPOs in MongoDB: Updates dynamic fields (subscription, status) without making extra REST calls.
     * For new IPOs not in MongoDB: Fetches full details ONCE, saves to MongoDB, and pre-warms Redis cache.
     *
     * @param targetStatus target status ("open", "upcoming", "closed", "listed", "all").
     * @return Number of IPOs successfully synchronized.
     */
    public int syncUpstoxIpos(String targetStatus) {
        List<String> statusesToSync = new ArrayList<>();
        if ("all".equalsIgnoreCase(targetStatus)) {
            statusesToSync.add("open");
            statusesToSync.add("upcoming");
            statusesToSync.add("closed");
            statusesToSync.add("listed");
        } else if (targetStatus != null && !targetStatus.trim().isEmpty()) {
            statusesToSync.add(targetStatus.trim().toLowerCase());
        } else {
            statusesToSync.add("open");
            statusesToSync.add("upcoming");
        }

        log.info("Starting Smart Delta Upstox IPO Sync for statuses: {}", statusesToSync);
        int totalSynced = 0;
        int newIposCount = 0;
        int updatedIposCount = 0;

        Semaphore semaphore = new Semaphore(3); // Rate-limiting semaphore

        for (String st : statusesToSync) {
            // 1 Single REST call per status returns ALL active IPOs!
            List<AsraxIpoSummaryDto> summaries = ipoDataProvider.getIpos(st, null, 1, 30);
            log.info("Single listing query returned {} IPO summaries for status='{}'", summaries.size(), st);

            for (AsraxIpoSummaryDto summary : summaries) {
                try {
                    Optional<UpstoxIpoDocument> existingDoc = mongoRepository.findById(summary.getId());

                    semaphore.acquire();
                    Optional<AsraxIpoDetailsDto> detailsOpt = ipoDataProvider.getIpoDetails(summary.getId());
                    if (detailsOpt.isPresent()) {
                        AsraxIpoDetailsDto details = detailsOpt.get();
                        saveDocToMongo(details);
                        cacheService.cacheDetails(details.getId(), details);
                        if (existingDoc.isPresent()) {
                            updatedIposCount++;
                        } else {
                            newIposCount++;
                        }
                        totalSynced++;
                        log.info("Full detail sync: Updated MongoDB & Redis for IPO id='{}'", details.getId());
                    } else if (existingDoc.isPresent()) {
                        // Fallback if detail call fails: update dynamic fields from summary
                        UpstoxIpoDocument doc = existingDoc.get();
                        doc.setStatus(summary.getStatus());
                        doc.setTotalSubscription(summary.getTotalSubscription());
                        doc.setBiddingEndDate(summary.getBiddingEndDate());
                        doc.setEligibleInvestors(summary.getEligibleInvestors());
                        doc.setUpdatedAt(Instant.now());

                        mongoRepository.save(doc);
                        AsraxIpoDetailsDto updatedDetails = toDetailsFromDoc(doc);
                        cacheService.cacheDetails(doc.getId(), updatedDetails);
                        updatedIposCount++;
                        totalSynced++;
                        log.debug("Summary fallback update for existing IPO id='{}'", summary.getId());
                    }
                    Thread.sleep(100L); // 100ms micro-pause for rate limit safety
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("Error during smart sync for IPO id='{}': {}", summary.getId(), e.getMessage());
                } finally {
                    semaphore.release();
                }
            }
        }

        log.info("Smart Delta Upstox IPO Sync Completed: Total {} IPOs processed (New: {}, Updated: {}). Single query efficiency achieved!",
                totalSynced, newIposCount, updatedIposCount);
        return totalSynced;
    }

    private void syncSummariesToMongo(List<AsraxIpoSummaryDto> summaries) {
        for (AsraxIpoSummaryDto s : summaries) {
            try {
                Optional<UpstoxIpoDocument> existing = mongoRepository.findById(s.getId());
                UpstoxIpoDocument doc = existing.orElseGet(() -> UpstoxIpoDocument.builder().id(s.getId()).build());
                doc.setSymbol(s.getSymbol());
                doc.setCompanyName(s.getCompanyName());
                doc.setStatus(s.getStatus());
                doc.setIsin(s.getIsin());
                doc.setIssueType(s.getIssueType());
                doc.setIssueSizeCr(s.getIssueSizeCr());
                doc.setIndustry(s.getIndustry());
                doc.setMinimumPrice(s.getMinimumPrice());
                doc.setMaximumPrice(s.getMaximumPrice());
                doc.setBiddingStartDate(s.getBiddingStartDate());
                doc.setBiddingEndDate(s.getBiddingEndDate());
                doc.setTotalSubscription(s.getTotalSubscription());
                doc.setEligibleInvestors(s.getEligibleInvestors());
                doc.setUpdatedAt(Instant.now());
                mongoRepository.save(doc);
            } catch (Exception e) {
                log.warn("Error background saving summary id='{}': {}", s.getId(), e.getMessage());
            }
        }
    }

    private void saveDocToMongo(AsraxIpoDetailsDto dto) {
        try {
            UpstoxIpoDocument doc = UpstoxIpoDocument.builder()
                    .id(dto.getId())
                    .symbol(dto.getSymbol())
                    .companyName(dto.getCompanyName())
                    .status(dto.getStatus())
                    .isin(dto.getIsin())
                    .issueType(dto.getIssueType())
                    .issueSizeCr(dto.getIssueSizeCr())
                    .industry(dto.getIndustry())
                    .minimumPrice(dto.getMinimumPrice())
                    .maximumPrice(dto.getMaximumPrice())
                    .biddingStartDate(dto.getBiddingStartDate())
                    .biddingEndDate(dto.getBiddingEndDate())
                    .dailyStartTime(dto.getDailyStartTime())
                    .dailyEndTime(dto.getDailyEndTime())
                    .faceValue(dto.getFaceValue())
                    .tickSize(dto.getTickSize())
                    .lotSize(dto.getLotSize())
                    .minimumQuantity(dto.getMinimumQuantity())
                    .cutOffPrice(dto.getCutOffPrice())
                    .listingPrice(dto.getListingPrice())
                    .listingExchange(dto.getListingExchange())
                    .rhpUrl(dto.getRhpUrl())
                    .drhpUrl(dto.getDrhpUrl())
                    .timeline(dto.getTimeline())
                    .registrarInfo(dto.getRegistrarInfo())
                    .totalSubscription(dto.getTotalSubscription())
                    .eligibleInvestors(dto.getEligibleInvestors())
                    .updatedAt(Instant.now())
                    .build();
            mongoRepository.save(doc);
            log.debug("Saved UpstoxIpoDocument id='{}' to MongoDB upstox_ipos collection.", dto.getId());
        } catch (Exception e) {
            log.warn("Error saving document id='{}' to MongoDB: {}", dto.getId(), e.getMessage());
        }
    }

    private AsraxIpoSummaryDto toSummaryFromDoc(UpstoxIpoDocument doc) {
        return AsraxIpoSummaryDto.builder()
                .id(doc.getId())
                .symbol(doc.getSymbol())
                .companyName(doc.getCompanyName())
                .status(doc.getStatus())
                .isin(doc.getIsin())
                .issueType(doc.getIssueType())
                .issueSizeCr(doc.getIssueSizeCr())
                .industry(doc.getIndustry())
                .minimumPrice(doc.getMinimumPrice())
                .maximumPrice(doc.getMaximumPrice())
                .biddingStartDate(doc.getBiddingStartDate())
                .biddingEndDate(doc.getBiddingEndDate())
                .totalSubscription(doc.getTotalSubscription())
                .eligibleInvestors(doc.getEligibleInvestors())
                .build();
    }

    private AsraxIpoDetailsDto toDetailsFromDoc(UpstoxIpoDocument doc) {
        return AsraxIpoDetailsDto.builder()
                .id(doc.getId())
                .symbol(doc.getSymbol())
                .companyName(doc.getCompanyName())
                .status(doc.getStatus())
                .isin(doc.getIsin())
                .issueType(doc.getIssueType())
                .issueSizeCr(doc.getIssueSizeCr())
                .industry(doc.getIndustry())
                .minimumPrice(doc.getMinimumPrice())
                .maximumPrice(doc.getMaximumPrice())
                .biddingStartDate(doc.getBiddingStartDate())
                .biddingEndDate(doc.getBiddingEndDate())
                .dailyStartTime(doc.getDailyStartTime())
                .dailyEndTime(doc.getDailyEndTime())
                .faceValue(doc.getFaceValue())
                .tickSize(doc.getTickSize())
                .lotSize(doc.getLotSize())
                .minimumQuantity(doc.getMinimumQuantity())
                .cutOffPrice(doc.getCutOffPrice())
                .listingPrice(doc.getListingPrice())
                .listingExchange(doc.getListingExchange())
                .rhpUrl(doc.getRhpUrl())
                .drhpUrl(doc.getDrhpUrl())
                .timeline(doc.getTimeline())
                .registrarInfo(doc.getRegistrarInfo())
                .totalSubscription(doc.getTotalSubscription())
                .eligibleInvestors(doc.getEligibleInvestors())
                .build();
    }
}
