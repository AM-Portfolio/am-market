package com.am.marketdata.scraper.cookie;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CookieCacheTest {

    @Mock
    private ObjectProvider<StringRedisTemplate> redisTemplateProvider;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOps;

    private CookieCache cookieCache;

    @BeforeEach
    void setUp() {
        when(redisTemplateProvider.getIfAvailable()).thenReturn(redisTemplate);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        cookieCache = new CookieCache(redisTemplateProvider, 60);
    }

    @Test
    void storeAndGet_roundTripsViaRedisAndL1() {
        when(valueOps.get(CookieCache.REDIS_COOKIE_KEY)).thenReturn(null);

        cookieCache.storeCookies("nsit=abc; nseappid=xyz");

        verify(valueOps).set(eq(CookieCache.REDIS_COOKIE_KEY), eq("nsit=abc; nseappid=xyz"), eq(60L),
                eq(TimeUnit.MINUTES));
        assertEquals("nsit=abc; nseappid=xyz", cookieCache.getCookies());
        assertTrue(cookieCache.status().present());
        assertEquals(2, cookieCache.status().cookieNames().size());
    }

    @Test
    void getCookiesOrThrow_whenEmpty_hasOpsHint() {
        when(valueOps.get(CookieCache.REDIS_COOKIE_KEY)).thenReturn(null);

        var ex = assertThrows(com.am.marketdata.scraper.exception.CookieException.class,
                cookieCache::getCookiesOrThrow);
        assertTrue(ex.getMessage().contains("PUT /v1/admin/nse/cookies"));
    }

    @Test
    void invalidate_clearsLocalAndRedis() {
        cookieCache.storeCookies("nsit=abc");
        when(valueOps.get(CookieCache.REDIS_COOKIE_KEY)).thenReturn("nsit=abc");

        cookieCache.invalidateCookies();

        verify(redisTemplate).delete(any(java.util.List.class));
        when(valueOps.get(CookieCache.REDIS_COOKIE_KEY)).thenReturn(null);
        assertFalse(cookieCache.status().present());
    }
}
