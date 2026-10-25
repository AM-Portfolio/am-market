package com.am.marketdata.service.global;

import de.focus_shift.jollyday.core.HolidayManager;
import de.focus_shift.jollyday.core.ManagerParameters;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Manages the global WebSocket voting flag ({@code market:websocket:vote:global})
 * based on the trading hours defined in {@code global-market-schedule.yml}.
 *
 * <p><b>WebSocket Voting Strategy:</b>
 * This service is NOT responsible for connecting or disconnecting the WebSocket directly.
 * It only writes to its dedicated Redis vote key:
 * <ul>
 *   <li>{@code market:websocket:vote:global = "true"} when any configured global market is open.</li>
 *   <li>{@code market:websocket:vote:global = "false"} when all global markets are closed.</li>
 * </ul>
 * The {@code StreamerManager} reconciler reads BOTH {@code vote:global} and
 * {@code vote:indian} and only disconnects when BOTH are false. This prevents
 * the Indian market scheduler from killing the WebSocket during US/European trading hours,
 * and vice versa.
 *
 * <p><b>DST Safety:</b>
 * Market hours are defined in each exchange's local timezone in the YAML file.
 * At runtime, {@code java.time.ZonedDateTime} with the exchange's IANA timezone ID
 * is used to evaluate open/close times. Java handles DST transitions automatically,
 * so the schedule remains correct year-round without manual YAML updates.
 *
 * <p><b>Holiday Handling:</b>
 * Standard national/bank holidays are handled by the Jollyday library.
 * Ad-hoc closures (natural disasters, national mourning) that Jollyday does not
 * know about are handled by the 30-minute no-tick circuit breaker in
 * {@code GlobalIndexCacheWriter}, which sets {@code market:global-suspended:<KEY>} in Redis.
 */
@Service
@RequiredArgsConstructor
public class GlobalMarketScheduleService {

    private static final Logger logger = LoggerFactory.getLogger(GlobalMarketScheduleService.class);

    /**
     * Redis key for the global market WebSocket vote.
     * Value is "true" when any global market is open; "false" otherwise.
     * The StreamerManager reconciler reads this alongside market:websocket:vote:indian.
     */
    public static final String GLOBAL_VOTE_KEY = "market:websocket:vote:global";

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    private final StringRedisTemplate redisTemplate;

    /** Loaded once on startup from global-market-schedule.yml. */
    private List<MarketEntry> markets = new ArrayList<>();

    /**
     * Loads the global market schedule from the YAML file on application startup.
     * This is done once and cached in-memory since the schedule rarely changes.
     */
    @PostConstruct
    public void loadSchedule() {
        try {
            Yaml yaml = new Yaml();
            InputStream is = getClass().getClassLoader().getResourceAsStream("global-market-schedule.yml");
            if (is == null) {
                logger.error("global-market-schedule.yml not found on classpath. " +
                        "Global WebSocket vote will default to false (no global streaming).");
                return;
            }

            Map<String, Object> root = yaml.load(is);
            @SuppressWarnings("unchecked")
            Map<String, Object> global = (Map<String, Object>) root.get("global");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rawMarkets = (List<Map<String, Object>>) global.get("markets");

            for (Map<String, Object> m : rawMarkets) {
                MarketEntry entry = new MarketEntry(
                        (String) m.get("symbol"),
                        (String) m.get("name"),
                        (String) m.get("instrumentKey"),
                        (String) m.get("exchange"),
                        (String) m.get("timezone"),
                        (String) m.get("marketOpen"),
                        (String) m.get("marketClose"),
                        (String) m.get("jollidayCountry"),
                        (String) m.get("influxAggregateOffset"),
                        m.get("quoteMaxAgeSeconds") instanceof Number n ? n.longValue() : 0L
                );
                markets.add(entry);
            }
            logger.info("Loaded {} global market schedules from global-market-schedule.yml", markets.size());
        } catch (Exception e) {
            logger.error("Failed to load global-market-schedule.yml: {}. " +
                    "Global streaming will not function correctly.", e.getMessage(), e);
        }
    }

    /**
     * Evaluates all configured global markets every 5 minutes and updates the Redis vote flag.
     *
     * <p>Runs on a fixed-rate schedule (configurable via {@code global.schedule.vote-interval-ms}).
     * The schedule is intentionally lightweight — it only reads in-memory market configs
     * and writes one Redis key. There are no database or network calls here.
     *
     * <p>Logic:
     * <ol>
     *   <li>If any market is currently open (after holiday check) → set vote = true.</li>
     *   <li>If all markets are closed → set vote = false.</li>
     * </ol>
     */
    @Scheduled(fixedRateString = "${global.schedule.vote-interval-ms:300000}")
    public void updateGlobalVote() {
        if (markets.isEmpty()) {
            logger.debug("No global markets configured. Keeping vote:global = false.");
            setVote(false);
            return;
        }

        boolean anyMarketOpen = markets.stream()
                .filter(MarketEntry::hasInstrument)
                .anyMatch(this::isMarketCurrentlyOpen);

        if (anyMarketOpen) {
            logger.info("[GlobalSchedule] At least one global market is open. Setting vote:global = true.");
        } else {
            logger.debug("[GlobalSchedule] All global markets are closed. Setting vote:global = false.");
        }

        setVote(anyMarketOpen);
    }

    /**
     * Checks if a specific market is currently open based on:
     * <ol>
     *   <li>The current time in the exchange's local timezone (DST-safe via ZonedDateTime).</li>
     *   <li>Whether today is a public holiday in the exchange's country (via Jollyday).</li>
     *   <li>Whether today is a weekend.</li>
     * </ol>
     *
     * @param market the market schedule entry to evaluate
     * @return true if the market is currently within its open/close window and not on holiday
     */
    public boolean isMarketCurrentlyOpen(MarketEntry market) {
        return isMarketCurrentlyOpen(market, Instant.now());
    }

    public boolean isMarketCurrentlyOpen(MarketEntry market, Instant instant) {
        try {
            ZoneId exchangeZone = ZoneId.of(market.timezone());
            ZonedDateTime now = instant.atZone(exchangeZone);
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();
            LocalTime open = LocalTime.parse(market.marketOpen(), TIME_FORMATTER);
            LocalTime close = LocalTime.parse(market.marketClose(), TIME_FORMATTER);
            boolean isOvernight = !close.isAfter(open);
            LocalDate sessionDate = isOvernight && currentTime.isBefore(close) ? today.minusDays(1) : today;
            java.time.DayOfWeek sessionDay = sessionDate.getDayOfWeek();
            if (sessionDay == java.time.DayOfWeek.SATURDAY || sessionDay == java.time.DayOfWeek.SUNDAY) {
                logger.debug("[GlobalSchedule] {} is closed: session date {} falls on a weekend", market.symbol(), sessionDate);
                return false;
            }
            if (isHoliday(market.jollidayCountry(), sessionDate)) {
                logger.info("[GlobalSchedule] {} is closed: public holiday in {} on {}",
                        market.symbol(), market.jollidayCountry(), sessionDate);
                return false;
            }

            boolean isOpen = isOvernight
                    ? !currentTime.isBefore(open) || currentTime.isBefore(close)
                    : !currentTime.isBefore(open) && currentTime.isBefore(close);

            logger.debug("[GlobalSchedule] {} ({}) - local time: {}, open: {}, close: {}, isOpen: {}",
                    market.symbol(), market.timezone(), currentTime, open, close, isOpen);

            return isOpen;
        } catch (Exception e) {
            // If timezone or time parsing fails, default to closed to be safe
            logger.error("[GlobalSchedule] Error evaluating market hours for {}: {}", market.symbol(), e.getMessage());
            return false;
        }
    }

    /**
     * Finds the most recent completed exchange session date, using the venue timezone,
     * configured session window, weekday rule and Jollyday calendar. A missing calendar
     * day beyond the bounded lookback returns empty rather than blessing an old quote.
     */
    public Optional<LocalDate> latestCompletedSessionDate(MarketEntry market, Instant instant) {
        try {
            ZonedDateTime localNow = instant.atZone(ZoneId.of(market.timezone()));
            LocalDate today = localNow.toLocalDate();
            LocalTime time = localNow.toLocalTime();
            LocalTime open = LocalTime.parse(market.marketOpen(), TIME_FORMATTER);
            LocalTime close = LocalTime.parse(market.marketClose(), TIME_FORMATTER);
            boolean overnight = !close.isAfter(open);
            LocalDate cursor;
            if (!overnight) {
                cursor = today;
                if (time.isBefore(close) || !isBusinessDay(market, today)) cursor = today.minusDays(1);
            } else if (!time.isBefore(open)) {
                cursor = today.minusDays(1); // today's overnight session is still in progress
            } else if (time.isBefore(close)) {
                cursor = today.minusDays(2); // previous day's overnight session is still in progress
            } else {
                cursor = today.minusDays(1); // overnight session closed earlier today
            }
            for (int attempts = 0; attempts < 15; attempts++, cursor = cursor.minusDays(1)) {
                if (isBusinessDay(market, cursor)) return Optional.of(cursor);
            }
        } catch (Exception e) {
            logger.warn("[GlobalSchedule] Unable to determine latest completed session for {}: {}",
                    market.symbol(), e.getMessage());
        }
        return Optional.empty();
    }

    /** True only when the provider event belongs to the latest completed venue session. */
    public boolean quoteBelongsToLatestCompletedSession(MarketEntry market, Instant quoteTime, Instant now) {
        try {
            ZonedDateTime event = quoteTime.atZone(ZoneId.of(market.timezone()));
            LocalTime eventTime = event.toLocalTime();
            LocalTime open = LocalTime.parse(market.marketOpen(), TIME_FORMATTER);
            LocalTime close = LocalTime.parse(market.marketClose(), TIME_FORMATTER);
            boolean overnight = !close.isAfter(open);
            LocalDate eventSessionDate;
            if (!overnight) {
                if (eventTime.isBefore(open) || eventTime.isAfter(close)) return false;
                eventSessionDate = event.toLocalDate();
            } else if (!eventTime.isBefore(open)) {
                eventSessionDate = event.toLocalDate();
            } else if (eventTime.isBefore(close)) {
                eventSessionDate = event.toLocalDate().minusDays(1);
            } else {
                return false;
            }
            return latestCompletedSessionDate(market, now)
                    .map(eventSessionDate::equals)
                    .orElse(false);
        } catch (Exception e) {
            logger.warn("[GlobalSchedule] Unable to validate quote session for {}: {}", market.symbol(), e.getMessage());
            return false;
        }
    }

    private boolean isBusinessDay(MarketEntry market, LocalDate date) {
        java.time.DayOfWeek day = date.getDayOfWeek();
        return day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY
                && !isHoliday(market.jollidayCountry(), date);
    }

    /**
     * Checks if a given date is a public holiday in the specified country
     * using the Jollyday library.
     *
     * <p>Jollyday covers standard national/bank holidays. It does NOT cover
     * ad-hoc closures (e.g., exchange closed for national mourning or disaster).
     * Those are handled separately by the 30-minute no-tick circuit breaker.
     *
     * @param countryCode ISO 3166-1 alpha-2 country code (e.g., "US", "GB", "JP")
     * @param date        the date to check
     * @return true if the date is a public holiday
     */
    private boolean isHoliday(String countryCode, LocalDate date) {
        try {
            HolidayManager hm = HolidayManager.getInstance(
                    ManagerParameters.create(countryCode.toLowerCase(Locale.ROOT)));
            return hm.isHoliday(date);
        } catch (Exception e) {
            // If Jollyday doesn't have data for this country, assume it's not a holiday
            logger.warn("[GlobalSchedule] Could not check holiday for country={}: {}. Assuming not a holiday.",
                    countryCode, e.getMessage());
            return false;
        }
    }

    /**
     * Writes the global market vote to Redis.
     *
     * <p>The key has NO expiry — it persists until the next scheduler cycle updates it.
     * This ensures the StreamerManager reconciler always sees a valid value even
     * if the scheduler has not run yet after a server restart.
     *
     * @param open true if any global market is open; false if all are closed
     */
    private void setVote(boolean open) {
        redisTemplate.opsForValue().set(GLOBAL_VOTE_KEY, String.valueOf(open));
    }

    /**
     * Checks the current value of the global vote flag.
     * Used by {@code StreamerManager} to decide whether to keep the WebSocket connected.
     *
     * @return true if the global vote is currently "true" in Redis
     */
    public boolean isGlobalVoteActive() {
        String val = redisTemplate.opsForValue().get(GLOBAL_VOTE_KEY);
        return "true".equalsIgnoreCase(val);
    }

    /**
     * Returns the list of all configured global market entries.
     * Used by {@code GlobalIndexSeeder} to populate the MongoDB config collection on startup.
     *
     * @return unmodifiable list of market schedule entries
     */
    public List<MarketEntry> getMarkets() {
        return List.copyOf(markets);
    }

    /**
     * Immutable record representing one row in {@code global-market-schedule.yml}.
     *
     * @param symbol           human-readable symbol (e.g., "DJI")
     * @param name             display name (e.g., "Dow Jones Industrial Average")
     * @param timezone         IANA timezone ID for DST-safe conversion (e.g., "America/New_York")
     * @param marketOpen       market open time in local timezone HH:mm (e.g., "09:30")
     * @param marketClose      market close time in local timezone HH:mm (e.g., "16:00")
     * @param jollidayCountry  ISO 3166-1 alpha-2 country code for Jollyday (e.g., "US")
     */
    public record MarketEntry(
            String symbol,
            String name,
            String instrumentKey,
            String exchange,
            String timezone,
            String marketOpen,
            String marketClose,
            String jollidayCountry,
            String influxAggregateOffset,
            long quoteMaxAgeSeconds
    ) {
        public boolean hasInstrument() {
            return instrumentKey != null && !instrumentKey.isBlank() && quoteMaxAgeSeconds > 0;
        }
    }
}
