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
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
                        (String) m.get("timezone"),
                        (String) m.get("marketOpen"),
                        (String) m.get("marketClose"),
                        (String) m.get("jollidayCountry")
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

        boolean anyMarketOpen = markets.stream().anyMatch(this::isMarketCurrentlyOpen);

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
        try {
            ZoneId exchangeZone = ZoneId.of(market.timezone());
            // Convert current UTC time to the exchange's local timezone — this is DST-safe
            ZonedDateTime now = ZonedDateTime.now(exchangeZone);
            LocalDate today = now.toLocalDate();
            LocalTime currentTime = now.toLocalTime();

            // Weekend check (most global exchanges are Mon-Fri)
            switch (now.getDayOfWeek()) {
                case SATURDAY, SUNDAY -> {
                    logger.debug("[GlobalSchedule] {} is closed: weekend in {}", market.symbol(), market.timezone());
                    return false;
                }
                default -> { /* weekday, continue */ }
            }

            // Holiday check via Jollyday library
            if (isHoliday(market.jollidayCountry(), today)) {
                logger.info("[GlobalSchedule] {} is closed: public holiday in {} on {}",
                        market.symbol(), market.jollidayCountry(), today);
                return false;
            }

            // Time range check using the exchange's local open/close times
            LocalTime open = LocalTime.parse(market.marketOpen(), TIME_FORMATTER);
            LocalTime close = LocalTime.parse(market.marketClose(), TIME_FORMATTER);
            boolean isOpen = !currentTime.isBefore(open) && currentTime.isBefore(close);

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
            String timezone,
            String marketOpen,
            String marketClose,
            String jollidayCountry
    ) {}
}
