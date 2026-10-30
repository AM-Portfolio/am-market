package com.am.marketdata.service.ipo;

import com.am.marketdata.common.model.ipo.AsraxIpoCalendarEntryDto;
import com.am.marketdata.common.model.ipo.AsraxIpoCalendarMonthDto;
import com.am.marketdata.common.model.ipo.AsraxIpoCountsDto;
import com.am.marketdata.common.model.ipo.AsraxIpoTimelineDto;
import com.am.marketdata.common.model.ipo.IpoCalendarDataFreshness;
import com.am.marketdata.common.model.ipo.IpoCalendarDisplayStatus;
import com.am.marketdata.service.model.IpoSyncMetaDocument;
import com.am.marketdata.service.repo.IpoSyncMetaRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.YearMonth;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
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
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
    private final IpoSyncMetaRepository ipoSyncMetaRepository;
    private final Clock clock;

    private static final String UPSTOX_ALL_SYNC_ID = "UPSTOX_ALL";
    private static final String UPSTOX_SOURCE = "UPSTOX";
    private static final int MAX_PAGE_LIMIT = 50;
    private static final int PAGE_SIZE = 30;

    @org.springframework.beans.factory.annotation.Autowired
    public UpstoxIpoService(
            @Qualifier("upstoxIpoProvider") IpoDataProvider ipoDataProvider,
            UpstoxIpoMongoRepository mongoRepository,
            UpstoxIpoCacheService cacheService,
            @org.springframework.lang.Nullable IpoSyncMetaRepository ipoSyncMetaRepository,
            @Qualifier("marketCalendarClock") @org.springframework.lang.Nullable Clock clock) {
        this.ipoDataProvider = ipoDataProvider;
        this.mongoRepository = mongoRepository;
        this.cacheService = cacheService;
        this.ipoSyncMetaRepository = ipoSyncMetaRepository;
        this.clock = clock != null ? clock : Clock.system(ZoneId.of("Asia/Kolkata"));
    }

    public UpstoxIpoService(
            IpoDataProvider ipoDataProvider,
            UpstoxIpoMongoRepository mongoRepository,
            UpstoxIpoCacheService cacheService) {
        this(ipoDataProvider, mongoRepository, cacheService, null, Clock.system(ZoneId.of("Asia/Kolkata")));
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
        boolean isAllSync = "all".equalsIgnoreCase(targetStatus);
        List<String> statusesToSync = new ArrayList<>();
        if (isAllSync) {
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

        log.info("Starting Upstox IPO Sync for statuses: {}", statusesToSync);
        int totalSynced = 0;
        int newIposCount = 0;
        int updatedIposCount = 0;
        boolean hasSyncErrors = false;
        String syncErrorMessage = null;
        Set<String> affectedYearMonths = new HashSet<>();

        // 1. Paginate and gather all summaries across requested statuses
        Map<String, AsraxIpoSummaryDto> deduplicatedSummaries = new LinkedHashMap<>();

        for (String st : statusesToSync) {
            int page = 1;
            boolean hasMorePages = true;

            while (hasMorePages && page <= MAX_PAGE_LIMIT) {
                try {
                    log.debug("Fetching Upstox IPO page {} (size {}) for status='{}'", page, PAGE_SIZE, st);
                    List<AsraxIpoSummaryDto> pageSummaries = ipoDataProvider.getIpos(st, null, page, PAGE_SIZE);

                    if (pageSummaries == null || pageSummaries.isEmpty()) {
                        hasMorePages = false;
                        log.debug("No more IPOs for status='{}' at page {}", st, page);
                    } else {
                        for (AsraxIpoSummaryDto s : pageSummaries) {
                            if (s != null && s.getId() != null && !s.getId().trim().isEmpty()) {
                                deduplicatedSummaries.putIfAbsent(s.getId().trim().toLowerCase(), s);
                            }
                        }
                        if (pageSummaries.size() < PAGE_SIZE) {
                            hasMorePages = false;
                        } else {
                            page++;
                        }
                    }
                } catch (Exception e) {
                    hasSyncErrors = true;
                    syncErrorMessage = String.format("Failed fetching IPO summaries for status='%s' page=%d: %s", st, page, e.getMessage());
                    log.error(syncErrorMessage, e);
                    hasMorePages = false; // Stop further pagination for this status on upstream failure
                }
            }

            if (page > MAX_PAGE_LIMIT) {
                log.warn("Safety cap of {} pages reached for status='{}'. Halting pagination.", MAX_PAGE_LIMIT, st);
                hasSyncErrors = true;
                syncErrorMessage = "Safety cap reached for status: " + st;
            }
        }

        log.info("Discovered {} unique IPOs across statuses {}", deduplicatedSummaries.size(), statusesToSync);

        // 2. Fetch details for each discovered IPO and persist to MongoDB idempotently
        Semaphore semaphore = new Semaphore(3);

        for (AsraxIpoSummaryDto summary : deduplicatedSummaries.values()) {
            try {
                semaphore.acquire();
                Optional<UpstoxIpoDocument> existingDocOpt = mongoRepository.findById(summary.getId());
                String oldListingMonth = existingDocOpt
                        .map(UpstoxIpoDocument::getTimeline)
                        .map(AsraxIpoTimelineDto::getListingDate)
                        .filter(d -> d != null && d.length() >= 7)
                        .map(d -> d.substring(0, 7))
                        .orElse(null);

                Optional<AsraxIpoDetailsDto> detailsOpt = ipoDataProvider.getIpoDetails(summary.getId());
                if (detailsOpt.isPresent()) {
                    AsraxIpoDetailsDto details = detailsOpt.get();
                    saveDocToMongo(details);
                    cacheService.cacheDetails(details.getId(), details);

                    if (existingDocOpt.isPresent()) {
                        updatedIposCount++;
                    } else {
                        newIposCount++;
                    }
                    totalSynced++;

                    String newListingMonth = Optional.ofNullable(details.getTimeline())
                            .map(AsraxIpoTimelineDto::getListingDate)
                            .filter(d -> d != null && d.length() >= 7)
                            .map(d -> d.substring(0, 7))
                            .orElse(null);

                    if (oldListingMonth != null) affectedYearMonths.add(oldListingMonth);
                    if (newListingMonth != null) affectedYearMonths.add(newListingMonth);
                } else if (existingDocOpt.isPresent()) {
                    // Fallback: update dynamic fields from summary
                    UpstoxIpoDocument doc = existingDocOpt.get();
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
                    if (oldListingMonth != null) affectedYearMonths.add(oldListingMonth);
                }
                Thread.sleep(100L); // micro-pause
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                hasSyncErrors = true;
                syncErrorMessage = "Sync interrupted: " + e.getMessage();
                break;
            } catch (Exception e) {
                log.warn("Error syncing IPO details for id='{}': {}", summary.getId(), e.getMessage());
            } finally {
                semaphore.release();
            }
        }

        log.info("Upstox IPO Sync Completed: Processed {} IPOs (New: {}, Updated: {}). Errors: {}",
                totalSynced, newIposCount, updatedIposCount, hasSyncErrors);

        // 3. Update Sync Metadata (only for complete 'all' runs with zero pagination failures)
        if (ipoSyncMetaRepository != null) {
            try {
                Optional<IpoSyncMetaDocument> existingMeta = ipoSyncMetaRepository.findById(UPSTOX_ALL_SYNC_ID);
                IpoSyncMetaDocument meta = existingMeta.orElseGet(() -> IpoSyncMetaDocument.builder()
                        .id(UPSTOX_ALL_SYNC_ID)
                        .source(UPSTOX_SOURCE)
                        .build());

                meta.setLastTrigger(targetStatus);
                meta.setLastCount(totalSynced);

                if (isAllSync && !hasSyncErrors) {
                    meta.setLastFullSyncAt(Instant.now(clock));
                    meta.setLastError(null);
                } else if (hasSyncErrors) {
                    meta.setLastError(syncErrorMessage != null ? syncErrorMessage : "Sync completed with errors");
                }
                ipoSyncMetaRepository.save(meta);
            } catch (Exception e) {
                log.warn("Failed to record sync metadata in MongoDB: {}", e.getMessage());
            }
        }

        // 4. Invalidate affected calendar months in Redis
        try {
            LocalDate todayInKolkata = LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")));
            affectedYearMonths.add(YearMonth.from(todayInKolkata).toString());
            affectedYearMonths.add(YearMonth.from(todayInKolkata).plusMonths(1).toString());
            cacheService.invalidateCalendarMonths(affectedYearMonths);
        } catch (Exception e) {
            log.warn("Failed to invalidate calendar cache post-sync: {}", e.getMessage());
        }

        // 5. Auto-recalculate summary counts
        try {
            recalculateAndCacheCounts();
        } catch (Exception e) {
            log.warn("Failed to recalculate IPO counts post-sync: {}", e.getMessage());
        }

        return totalSynced;
    }

    /**
     * Retrieves monthly calendar of IPO listing events for the current or immediately following month in Asia/Kolkata.
     */
    public AsraxIpoCalendarMonthDto getIpoCalendarMonth(int year, int month) {
        if (month < 1 || month > 12) {
            throw new IllegalArgumentException("Month must be between 1 and 12, but was " + month);
        }

        LocalDate todayInKolkata = LocalDate.now(clock.withZone(ZoneId.of("Asia/Kolkata")));
        YearMonth currentYm = YearMonth.from(todayInKolkata);
        YearMonth nextYm = currentYm.plusMonths(1);
        YearMonth requestedYm = YearMonth.of(year, month);

        if (!requestedYm.equals(currentYm) && !requestedYm.equals(nextYm)) {
            throw new IllegalArgumentException(String.format(
                    "Calendar month %s is outside the allowed range. Only current month (%s) and immediately following month (%s) in Asia/Kolkata are permitted.",
                    requestedYm, currentYm, nextYm));
        }

        // 1. Try Redis cache first
        Optional<AsraxIpoCalendarMonthDto> cached = cacheService.getCachedCalendarMonth(year, month);
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Determine data freshness from ipo_sync_meta
        IpoCalendarDataFreshness freshness = IpoCalendarDataFreshness.UNAVAILABLE;
        String lastSuccessfulSyncStr = null;

        if (ipoSyncMetaRepository != null) {
            try {
                Optional<IpoSyncMetaDocument> metaOpt = ipoSyncMetaRepository.findById(UPSTOX_ALL_SYNC_ID);
                if (metaOpt.isPresent() && metaOpt.get().getLastFullSyncAt() != null) {
                    Instant lastSync = metaOpt.get().getLastFullSyncAt();
                    lastSuccessfulSyncStr = lastSync.toString();
                    Duration age = Duration.between(lastSync, Instant.now(clock));
                    if (age.toHours() <= 24) {
                        freshness = IpoCalendarDataFreshness.FRESH;
                    } else {
                        freshness = IpoCalendarDataFreshness.STALE;
                    }
                }
            } catch (Exception e) {
                log.warn("Failed reading sync metadata: {}", e.getMessage());
            }
        }

        // If no complete sync has ever succeeded, return UNAVAILABLE with empty entries
        if (freshness == IpoCalendarDataFreshness.UNAVAILABLE) {
            return AsraxIpoCalendarMonthDto.builder()
                    .year(year)
                    .month(month)
                    .entries(Collections.emptyList())
                    .dataFreshness(IpoCalendarDataFreshness.UNAVAILABLE)
                    .lastSuccessfulSyncAt(null)
                    .build();
        }

        // 3. Query MongoDB for issues with timeline.listingDate in requested month
        String startDateStr = requestedYm.atDay(1).format(DateTimeFormatter.ISO_LOCAL_DATE);
        String endDateStr = requestedYm.atEndOfMonth().format(DateTimeFormatter.ISO_LOCAL_DATE);

        List<UpstoxIpoDocument> docs;
        try {
            docs = mongoRepository.findByTimelineListingDateBetween(startDateStr, endDateStr);
        } catch (Exception e) {
            log.error("Failed querying MongoDB for IPO calendar month {}: {}", requestedYm, e.getMessage(), e);
            docs = Collections.emptyList();
        }

        // 4. Map documents to calendar entries according to domain business rules
        List<AsraxIpoCalendarEntryDto> entries = new ArrayList<>();

        for (UpstoxIpoDocument doc : docs) {
            if (doc == null || doc.getId() == null || doc.getId().trim().isEmpty()) {
                continue;
            }

            // Company name check: skip and flag if null/blank
            if (doc.getCompanyName() == null || doc.getCompanyName().trim().isEmpty()) {
                log.warn("Data quality warning: IPO id='{}' has null/blank companyName. Omitted from calendar.", doc.getId());
                continue;
            }

            // Timeline and listingDate check
            if (doc.getTimeline() == null || doc.getTimeline().getListingDate() == null || doc.getTimeline().getListingDate().trim().isEmpty()) {
                continue;
            }

            String listingDateStr = doc.getTimeline().getListingDate().trim();
            LocalDate listingDate;
            try {
                listingDate = LocalDate.parse(listingDateStr, DateTimeFormatter.ISO_LOCAL_DATE);
            } catch (Exception e) {
                log.warn("Data quality warning: IPO id='{}' has invalid listingDate '{}'. Omitted.", doc.getId(), listingDateStr);
                continue;
            }

            // Must fall strictly within requested month
            if (YearMonth.from(listingDate).compareTo(requestedYm) != 0) {
                continue;
            }

            // Status normalization & validation
            String rawStatus = doc.getStatus() != null ? doc.getStatus().trim().toLowerCase() : "";
            IpoCalendarDisplayStatus displayStatus;

            if ("listed".equals(rawStatus)) {
                displayStatus = IpoCalendarDisplayStatus.LISTED;
                if (listingDate.isAfter(todayInKolkata)) {
                    log.warn("Data inconsistency: IPO id='{}' is marked 'listed' with future listingDate '{}'",
                            doc.getId(), listingDateStr);
                }
            } else if ("open".equals(rawStatus) || "upcoming".equals(rawStatus) || "closed".equals(rawStatus)) {
                if (listingDate.isBefore(todayInKolkata)) {
                    log.warn("Data quality warning: IPO id='{}' has status '{}' but listingDate '{}' is in the past. Omitted.",
                            doc.getId(), rawStatus, listingDateStr);
                    continue;
                }
                displayStatus = IpoCalendarDisplayStatus.UPCOMING;
            } else {
                log.warn("Data quality warning: IPO id='{}' has unknown/null status '{}'. Omitted from calendar.",
                        doc.getId(), doc.getStatus());
                continue;
            }

            entries.add(AsraxIpoCalendarEntryDto.builder()
                    .id(doc.getId().trim())
                    .companyName(doc.getCompanyName().trim())
                    .status(displayStatus)
                    .listingDate(listingDateStr)
                    .listingPrice(doc.getListingPrice())
                    .listingExchange(doc.getListingExchange())
                    .build());
        }

        // 5. Sort deterministically by listingDate ascending, then stable id ascending
        entries.sort(Comparator
                .comparing(AsraxIpoCalendarEntryDto::getListingDate)
                .thenComparing(AsraxIpoCalendarEntryDto::getId));

        AsraxIpoCalendarMonthDto monthDto = AsraxIpoCalendarMonthDto.builder()
                .year(year)
                .month(month)
                .entries(entries)
                .dataFreshness(freshness)
                .lastSuccessfulSyncAt(lastSuccessfulSyncStr)
                .build();

        // 6. Cache and return
        cacheService.cacheCalendarMonth(year, month, monthDto);
        return monthDto;
    }

    /**
     * Gets summary counts of IPOs categorized by lifecycle status.
     * Hierarchy: 1) Redis Cache -> 2) MongoDB Fallback / Recalculation.
     */
    public AsraxIpoCountsDto getIpoCounts() {
        // 1. Try Redis Cache
        Optional<AsraxIpoCountsDto> cached = cacheService.getCachedCounts();
        if (cached.isPresent()) {
            return cached.get();
        }

        // 2. Fallback to recalculating from MongoDB
        return recalculateAndCacheCounts();
    }

    /**
     * Recalculates IPO counts directly from MongoDB upstox_ipos collection,
     * updates Redis cache, and returns the populated DTO.
     */
    public AsraxIpoCountsDto recalculateAndCacheCounts() {
        try {
            String todayStr = LocalDate.now(ZoneId.of("Asia/Kolkata")).format(DateTimeFormatter.ISO_LOCAL_DATE);

            long openCount = mongoRepository.countByStatus("open");
            long upcomingCount = mongoRepository.countByStatus("upcoming");
            long closedCount = mongoRepository.countByStatus("closed");
            long listedCount = mongoRepository.countByStatus("listed");
            long closingTodayCount = mongoRepository.countByStatusAndBiddingEndDate("open", todayStr);
            long totalCount = mongoRepository.count();

            AsraxIpoCountsDto counts = AsraxIpoCountsDto.builder()
                    .open(openCount)
                    .upcoming(upcomingCount)
                    .closed(closedCount)
                    .closingToday(closingTodayCount)
                    .listed(listedCount)
                    .total(totalCount)
                    .lastSyncedAt(Instant.now().toString())
                    .build();

            log.info("Recalculated IPO counts from MongoDB: open={}, upcoming={}, closed={}, closingToday={}, listed={}, total={}",
                    openCount, upcomingCount, closedCount, closingTodayCount, listedCount, totalCount);

            cacheService.cacheCounts(counts);
            return counts;
        } catch (Exception e) {
            log.error("Error recalculating IPO counts: {}", e.getMessage(), e);
            return AsraxIpoCountsDto.builder()
                    .lastSyncedAt(Instant.now().toString())
                    .build();
        }
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
