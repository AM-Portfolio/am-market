package com.am.marketdata.redis.config;

import com.am.libraries.featureflag.service.GrowthBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationTargetException;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * Custom StringRedisTemplate subclass that intercepts Redis calls and returns safe fallback responses
 * (simulating cache misses) when the "redis-enabled" feature flag is disabled.
 * Extends StringRedisTemplate directly to override Spring Boot's auto-configured StringRedisTemplate bean.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class FeatureFlaggedStringRedisTemplate extends StringRedisTemplate {

    private final GrowthBookService growthBookService;
    private final RedisAvailabilityGuard redisAvailabilityGuard;
    private final String flagKey = "redis-enabled";
    private volatile Boolean lastState = null;

    private ValueOperations<String, String> valueOpsProxy;
    private HashOperations<String, Object, Object> hashOpsProxy;

    public FeatureFlaggedStringRedisTemplate(GrowthBookService growthBookService,
            RedisAvailabilityGuard redisAvailabilityGuard) {
        this.growthBookService = growthBookService;
        this.redisAvailabilityGuard = redisAvailabilityGuard;
    }

    @Override
    public ValueOperations<String, String> opsForValue() {
        if (valueOpsProxy == null) {
            ValueOperations<String, String> realOps = super.opsForValue();
            valueOpsProxy = (ValueOperations<String, String>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    (proxy, method, args) -> {
                        if (!isRedisEnabled()) {
                            return handleDisabledOps(method.getName(), args, method.getReturnType());
                        }
                        return invokeWithGuard(() -> method.invoke(realOps, args));
                    }
            );
        }
        return valueOpsProxy;
    }

    @Override
    public <HK, HV> HashOperations<String, HK, HV> opsForHash() {
        if (hashOpsProxy == null) {
            HashOperations<String, Object, Object> realOps = super.opsForHash();
            hashOpsProxy = (HashOperations<String, Object, Object>) Proxy.newProxyInstance(
                    HashOperations.class.getClassLoader(),
                    new Class<?>[]{HashOperations.class},
                    (proxy, method, args) -> {
                        if (!isRedisEnabled()) {
                            return handleDisabledOps(method.getName(), args, method.getReturnType());
                        }
                        return invokeWithGuard(() -> method.invoke(realOps, args));
                    }
            );
        }
        return (HashOperations<String, HK, HV>) hashOpsProxy;
    }

    @Override
    public Boolean expire(String key, long timeout, TimeUnit unit) {
        if (!isRedisEnabled()) return false;
        return invokeWithGuard(() -> super.expire(key, timeout, unit));
    }

    @Override
    public Boolean expire(String key, Duration timeout) {
        if (!isRedisEnabled()) return false;
        return invokeWithGuard(() -> super.expire(key, timeout));
    }

    @Override
    public Boolean delete(String key) {
        if (!isRedisEnabled()) return false;
        return invokeWithGuard(() -> super.delete(key));
    }

    @Override
    public Long delete(Collection<String> keys) {
        if (!isRedisEnabled()) return 0L;
        return invokeWithGuard(() -> super.delete(keys));
    }

    @Override
    public Boolean hasKey(String key) {
        if (!isRedisEnabled()) return false;
        return invokeWithGuard(() -> super.hasKey(key));
    }

    @Override
    public <T> T execute(RedisCallback<T> action) {
        if (!isRedisEnabled()) return null;
        return invokeWithGuard(() -> super.execute(action));
    }

    @Override
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection) {
        if (!isRedisEnabled()) return null;
        return invokeWithGuard(() -> super.execute(action, exposeConnection));
    }

    @Override
    public <T> T execute(RedisCallback<T> action, boolean exposeConnection, boolean pipeline) {
        if (!isRedisEnabled()) return null;
        return invokeWithGuard(() -> super.execute(action, exposeConnection, pipeline));
    }

    @Override
    public <T> T execute(SessionCallback<T> session) {
        if (!isRedisEnabled()) return null;
        return invokeWithGuard(() -> super.execute(session));
    }

    @Override
    public List<Object> executePipelined(RedisCallback<?> action) {
        if (!isRedisEnabled()) return Collections.emptyList();
        return invokeWithGuard(() -> super.executePipelined(action));
    }

    @Override
    public List<Object> executePipelined(RedisCallback<?> action, RedisSerializer<?> resultSerializer) {
        if (!isRedisEnabled()) return Collections.emptyList();
        return invokeWithGuard(() -> super.executePipelined(action, resultSerializer));
    }

    @Override
    public List<Object> executePipelined(SessionCallback<?> session) {
        if (!isRedisEnabled()) return Collections.emptyList();
        return invokeWithGuard(() -> super.executePipelined(session));
    }

    @Override
    public List<Object> executePipelined(SessionCallback<?> session, RedisSerializer<?> resultSerializer) {
        if (!isRedisEnabled()) return Collections.emptyList();
        return invokeWithGuard(() -> super.executePipelined(session, resultSerializer));
    }

    @Override
    public <T> T execute(RedisScript<T> script, java.util.List<String> keys, Object... args) {
        if (!isRedisEnabled()) return null;
        return invokeWithGuard(() -> super.execute(script, keys, args));
    }

    public boolean isRedisEnabled() {
        return redisAvailabilityGuard.allowRequest() && isFeatureFlagEnabled();
    }

    private boolean isFeatureFlagEnabled() {
        if ("true".equalsIgnoreCase(System.getenv("REDIS_FORCE_ENABLED"))
                || "true".equalsIgnoreCase(System.getProperty("REDIS_FORCE_ENABLED"))) {
            return true;
        }
        if (growthBookService == null) {
            return true;
        }
        if (!growthBookService.isConnected()) {
            if (lastState == null || !lastState) {
                lastState = true;
                log.info("[Redis-FF] GrowthBook SSE disconnected; keeping Redis caching ENABLED by default.");
            }
            return true;
        }
        final boolean enabled;
        try {
            enabled = growthBookService.isOn(flagKey);
        } catch (Exception exception) {
            // Redis is an accelerator, but disabling it on a feature-flag transport
            // failure turns every request into an expensive database/provider request.
            // Keep Redis enabled when the flag service is unavailable; Redis itself is
            // still protected by RedisAvailabilityGuard.
            log.warn("[Redis-FF] GrowthBook unavailable; defaulting Redis to ENABLED", exception);
            lastState = true;
            return true;
        }
        if (lastState == null || lastState != enabled) {
            lastState = enabled;
            if (enabled) {
                log.info("[Redis-FF] Redis caching is now ENABLED via GrowthBook feature flag.");
            } else {
                log.warn("[Redis-FF] Redis caching is now DISABLED via GrowthBook feature flag. Bypassing all cache operations.");
            }
        }
        return enabled;
    }

    private <T> T invokeWithGuard(ThrowingSupplier<T> action) {
        try {
            T result = action.get();
            redisAvailabilityGuard.recordSuccess();
            return result;
        } catch (InvocationTargetException e) {
            redisAvailabilityGuard.recordFailure(e.getCause());
            throw new RedisCacheOperationException(e.getCause());
        } catch (Throwable e) {
            redisAvailabilityGuard.recordFailure(e);
            throw new RedisCacheOperationException(e);
        }
    }

    @FunctionalInterface
    private interface ThrowingSupplier<T> {
        T get() throws Throwable;
    }

    private static final class RedisCacheOperationException extends RuntimeException {
        private RedisCacheOperationException(Throwable cause) {
            super(cause);
        }
    }

    private Object handleDisabledOps(String methodName, Object[] args, Class<?> returnType) {
        if (returnType.equals(void.class)) {
            return null;
        }
        if (returnType.equals(Boolean.class) || returnType.equals(boolean.class)) {
            return false;
        }
        if (returnType.equals(Long.class) || returnType.equals(long.class)) {
            return 0L;
        }
        if (List.class.isAssignableFrom(returnType)) {
            if ("multiGet".equals(methodName) && args != null && args.length > 0 && args[0] instanceof Collection) {
                return Collections.nCopies(((Collection<?>) args[0]).size(), null);
            }
            return Collections.emptyList();
        }
        if (Set.class.isAssignableFrom(returnType)) {
            return Collections.emptySet();
        }
        if (Map.class.isAssignableFrom(returnType)) {
            return Collections.emptyMap();
        }
        return null;
    }
}
