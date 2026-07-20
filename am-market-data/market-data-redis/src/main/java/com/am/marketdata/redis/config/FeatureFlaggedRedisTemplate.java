package com.am.marketdata.redis.config;

import com.am.libraries.featureflag.service.GrowthBookService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.lang.reflect.Proxy;
import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Custom RedisTemplate subclass that intercepts Redis calls and returns safe fallback responses
 * (simulating cache misses) when the "redis-enabled" feature flag is disabled.
 */
@Slf4j
@SuppressWarnings("unchecked")
public class FeatureFlaggedRedisTemplate<K, V> extends RedisTemplate<K, V> {

    private final GrowthBookService growthBookService;
    private final String flagKey = "redis-enabled";
    private volatile Boolean lastState = null;

    private ValueOperations<K, V> valueOpsProxy;
    private HashOperations<K, Object, Object> hashOpsProxy;

    public FeatureFlaggedRedisTemplate(GrowthBookService growthBookService) {
        this.growthBookService = growthBookService;
    }

    private boolean isRedisEnabled() {
        boolean enabled = growthBookService.isOn(flagKey);
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

    @Override
    public ValueOperations<K, V> opsForValue() {
        if (valueOpsProxy == null) {
            ValueOperations<K, V> realOps = super.opsForValue();
            valueOpsProxy = (ValueOperations<K, V>) Proxy.newProxyInstance(
                    ValueOperations.class.getClassLoader(),
                    new Class<?>[]{ValueOperations.class},
                    (proxy, method, args) -> {
                        if (!isRedisEnabled()) {
                            return handleDisabledOps(method.getName(), args, method.getReturnType());
                        }
                        return method.invoke(realOps, args);
                    }
            );
        }
        return valueOpsProxy;
    }

    @Override
    public <HK, HV> HashOperations<K, HK, HV> opsForHash() {
        if (hashOpsProxy == null) {
            HashOperations<K, Object, Object> realOps = super.opsForHash();
            hashOpsProxy = (HashOperations<K, Object, Object>) Proxy.newProxyInstance(
                    HashOperations.class.getClassLoader(),
                    new Class<?>[]{HashOperations.class},
                    (proxy, method, args) -> {
                        if (!isRedisEnabled()) {
                            return handleDisabledOps(method.getName(), args, method.getReturnType());
                        }
                        return method.invoke(realOps, args);
                    }
            );
        }
        return (HashOperations<K, HK, HV>) hashOpsProxy;
    }

    @Override
    public Boolean expire(K key, long timeout, TimeUnit unit) {
        if (!isRedisEnabled()) return false;
        return super.expire(key, timeout, unit);
    }

    @Override
    public Boolean expire(K key, Duration timeout) {
        if (!isRedisEnabled()) return false;
        return super.expire(key, timeout);
    }

    @Override
    public Boolean delete(K key) {
        if (!isRedisEnabled()) return false;
        return super.delete(key);
    }

    @Override
    public Long delete(Collection<K> keys) {
        if (!isRedisEnabled()) return 0L;
        return super.delete(keys);
    }

    @Override
    public Boolean hasKey(K key) {
        if (!isRedisEnabled()) return false;
        return super.hasKey(key);
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
            // For multiGet, return a list of nulls matching the input size to simulate cache misses
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
