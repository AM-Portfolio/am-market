package com.am.marketdata.common.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declarative annotation to enable centralized, high-speed Redis-backed smart recommendations.
 * 
 * <p>When applied to a search controller or service method, the intercepting AOP aspect:
 * <ul>
 *   <li>Sanitizes search queries (strips quotation marks, extra whitespace).</li>
 *   <li>Checks the distributed Redis cache for instant (&lt;1ms) prefix lookups.</li>
 *   <li>Dynamically filters out delisted / suspended securities.</li>
 *   <li>Ranks active candidates by Market Capitalization tier (Large Cap &gt; Mid Cap &gt; Small Cap).</li>
 *   <li>Falls back to legacy execution if {@code smartRecommendations} is false or omitted.</li>
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SmartRecommendation {

    /**
     * Target instrument segment or category (e.g., "STOCKS", "FNO", "MUTUAL_FUNDS", "ETF", "ALL").
     */
    String category() default "STOCKS";

    /**
     * Maximum number of recommendation results to return.
     */
    int limit() default 8;

    /**
     * Whether to boost high-market-cap blue chips (NIFTY 50 / Large Cap) to the top of results.
     */
    boolean prioritizeMarketCap() default true;

    /**
     * Whether to suppress delisted, suspended, or inactive securities (e.g. delisted 2023 HDFC Ltd).
     */
    boolean excludeDelisted() default true;
}
