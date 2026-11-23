package com.sambarspicy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sambarspicy.controller.PublicMenuController.PublicMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;
import java.util.List;

@Service
public class PublicMenuCache {
    private static final Logger log = LoggerFactory.getLogger(PublicMenuCache.class);
    private static final Duration TTL = Duration.ofSeconds(30);
    private final String revisionKey;
    private final String dataKey;
    private final boolean enabled;
    private final ObjectProvider<StringRedisTemplate> redisProvider;
    private final ObjectMapper mapper;
    private volatile long unavailableUntil;
    private final Object flightLock = new Object();
    private CompletableFuture<PublicMenu> flight;

    public PublicMenuCache(@Value("${app.public-menu-cache.enabled:false}") boolean enabled,
                           ObjectProvider<StringRedisTemplate> redisProvider, ObjectMapper mapper,
                           @Value("${app.public-menu-cache.namespace:sambar}") String namespace) {
        if (!namespace.matches("[A-Za-z0-9_-]{1,64}"))
            throw new IllegalArgumentException("Public-menu cache namespace must contain only letters, digits, underscores or hyphens.");
        this.enabled = enabled;
        this.redisProvider = redisProvider;
        this.mapper = mapper;
        this.revisionKey = namespace + ":public-menu:v1:revision";
        this.dataKey = namespace + ":public-menu:v1:data:";
    }

    public PublicMenu get(Supplier<PublicMenu> loader) {
        CompletableFuture<PublicMenu> current;
        boolean leader;
        synchronized (flightLock) {
            leader = flight == null;
            if (leader) flight = new CompletableFuture<>();
            current = flight;
        }
        if (!leader) {
            try { return current.join(); }
            catch (CompletionException failure) {
                if (failure.getCause() instanceof RuntimeException cause) throw cause;
                if (failure.getCause() instanceof Error cause) throw cause;
                throw failure;
            }
        }
        try {
            PublicMenu menu = readMenu(loader);
            current.complete(menu);
            return menu;
        } catch (RuntimeException | Error failure) {
            current.completeExceptionally(failure);
            throw failure;
        } finally {
            synchronized (flightLock) { if (flight == current) flight = null; }
        }
    }

    private PublicMenu readMenu(Supplier<PublicMenu> loader) {
        if (!enabled || System.nanoTime() < unavailableUntil) return loader.get();
        StringRedisTemplate redis;
        String revision;
        try {
            redis = redisProvider.getIfAvailable();
            if (redis == null) return loader.get();
            revision = redis.opsForValue().get(revisionKey);
            if (revision == null) revision = "0";
            String cached = redis.opsForValue().get(dataKey + revision);
            if (cached != null) {
                try {
                    PublicMenu result = mapper.readValue(cached, PublicMenu.class);
                    if (result != null && result.store() != null && result.store().name() != null &&
                            result.categories() != null && result.items() != null) return result;
                    log.warn("Ignoring incomplete public-menu cache data.");
                }
                catch (JsonProcessingException invalid) { log.warn("Ignoring invalid public-menu cache data."); }
            }
        } catch (RuntimeException failure) {
            markUnavailable();
            return loader.get();
        }
        PublicMenu menu = loader.get();
        try {
            // Keep the revision captured before the database read. If a writer
            // commits during that read, its increment makes this entry unreachable.
            redis.opsForValue().set(dataKey + revision, mapper.writeValueAsString(menu), TTL);
        } catch (RuntimeException | JsonProcessingException failure) {
            markUnavailable();
        }
        return menu;
    }

    /** Bounded catalog variants share the same post-commit revision as the public menu. */
    public <T> List<T> getCatalog(String variant, TypeReference<List<T>> type, Supplier<List<T>> loader) {
        if (!enabled || System.nanoTime() < unavailableUntil ||
                (org.springframework.transaction.support.TransactionSynchronizationManager.isActualTransactionActive() &&
                 !org.springframework.transaction.support.TransactionSynchronizationManager.isCurrentTransactionReadOnly())) {
            return loader.get();
        }
        StringRedisTemplate redis;
        String revision;
        String key;
        try {
            redis = redisProvider.getIfAvailable();
            if (redis == null) return loader.get();
            revision = redis.opsForValue().get(revisionKey);
            if (revision == null) revision = "0";
            key = dataKey + revision + ":catalog:" + variant;
            String cached = redis.opsForValue().get(key);
            if (cached != null) {
                try {
                    List<T> value = mapper.readValue(cached, type);
                    if (value != null && !value.contains(null)) return value;
                } catch (JsonProcessingException invalid) {
                    log.warn("Ignoring invalid catalog cache data.");
                }
            }
        } catch (RuntimeException failure) {
            markUnavailable();
            return loader.get();
        }
        List<T> result = loader.get();
        try {
            redis.opsForValue().set(key, mapper.writeValueAsString(result), TTL);
        } catch (RuntimeException | JsonProcessingException failure) {
            markUnavailable();
        }
        return result;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void invalidate(PublicMenuChanged ignored) {
        synchronized (flightLock) {
            // Don't let a new load read the old revision while invalidation is in progress.
            flight = null;
            if (!enabled) return;
            try {
                StringRedisTemplate redis = redisProvider.getIfAvailable();
                if (redis != null) redis.opsForValue().increment(revisionKey);
            } catch (RuntimeException failure) {
                // A cache outage must not fail an already committed catalog change.
                markUnavailable();
            }
        }
    }

    private void markUnavailable() {
        unavailableUntil = System.nanoTime() + Duration.ofSeconds(10).toNanos();
        log.warn("Public-menu Redis cache unavailable; reading from the database.");
    }
}
