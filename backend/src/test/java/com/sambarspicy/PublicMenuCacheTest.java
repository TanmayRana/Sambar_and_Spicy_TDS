package com.sambarspicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sambarspicy.controller.PublicMenuController.PublicMenu;
import com.sambarspicy.controller.PublicMenuController.Store;
import com.sambarspicy.service.PublicMenuCache;
import com.sambarspicy.service.PublicMenuChanged;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class PublicMenuCacheTest {
    private static final String REVISION = "sambar:public-menu:v1:revision";
    private static final String DATA = "sambar:public-menu:v1:data:";
    private static PublicMenu menu(String name) { return new PublicMenu(new Store(name, "", ""), List.of(), List.of()); }

    @SuppressWarnings("unchecked")
    @Test
    void catalogCacheUsesSeparateVariantsAndFallsBackOnRedisFailure() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        Map<String, String> data = new HashMap<>();
        when(values.get(anyString())).thenAnswer(call -> data.get(call.getArgument(0)));
        doAnswer(call -> { data.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), eq(Duration.ofSeconds(30)));
        PublicMenuCache cache = new PublicMenuCache(true, provider, new ObjectMapper(), "sambar");
        TypeReference<List<String>> type = new TypeReference<>() {};
        assertEquals(List.of("all"), cache.getCatalog("categories-all", type, () -> List.of("all")));
        assertEquals(List.of("all"), cache.getCatalog("categories-all", type, () -> List.of("wrong")));
        assertEquals(List.of("active"), cache.getCatalog("categories-active", type, () -> List.of("active")));
        when(values.get(REVISION)).thenThrow(new IllegalStateException("offline"));
        assertEquals(List.of("database"), cache.getCatalog("categories-all", type, () -> List.of("database")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void simultaneousVisitorsShareAnInFlightDatabaseReadAndCanRetryFailures() throws Exception {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        PublicMenuCache cache = new PublicMenuCache(false, provider, new ObjectMapper(), "sambar");
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        AtomicInteger loads = new AtomicInteger();
        CompletableFuture<PublicMenu> first = CompletableFuture.supplyAsync(() -> cache.get(() -> {
            loads.incrementAndGet(); entered.countDown();
            try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timed out"); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
            return menu("Shared");
        }));
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        CountDownLatch started = new CountDownLatch(1);
        CompletableFuture<PublicMenu> second = CompletableFuture.supplyAsync(() -> {
            started.countDown();
            return cache.get(() -> { loads.incrementAndGet(); return menu("Unexpected"); });
        });
        assertTrue(started.await(5, TimeUnit.SECONDS));
        try {
            // The second visitor must remain blocked while the one database read is in progress.
            assertThrows(java.util.concurrent.TimeoutException.class, () -> second.get(100, TimeUnit.MILLISECONDS));
        } finally { release.countDown(); }
        assertEquals("Shared", first.get(5, TimeUnit.SECONDS).store().name());
        assertEquals("Shared", second.get(5, TimeUnit.SECONDS).store().name());
        assertEquals(1, loads.get());
        assertThrows(IllegalStateException.class, () -> cache.get(() -> { throw new IllegalStateException("database failed"); }));
        assertEquals("Recovered", cache.get(() -> menu("Recovered")).store().name());
    }

    @SuppressWarnings("unchecked")
    @Test
    void cachesAndInvalidatesWithoutRepopulatingAnOldRevision() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        Map<String, String> data = new HashMap<>();
        when(values.get(anyString())).thenAnswer(call -> data.get(call.getArgument(0)));
        doAnswer(call -> { data.put(call.getArgument(0), call.getArgument(1)); return null; })
                .when(values).set(anyString(), anyString(), eq(Duration.ofSeconds(30)));
        when(values.increment(REVISION)).thenAnswer(call -> {
            long next = Long.parseLong(data.getOrDefault(REVISION, "0")) + 1;
            data.put(REVISION, Long.toString(next));
            return next;
        });
        PublicMenuCache cache = new PublicMenuCache(true, provider, new ObjectMapper(), "sambar");
        AtomicInteger loads = new AtomicInteger();
        assertEquals("Old", cache.get(() -> { loads.incrementAndGet(); return menu("Old"); }).store().name());
        assertEquals("Old", cache.get(() -> { loads.incrementAndGet(); return menu("Wrong"); }).store().name());
        assertEquals(1, loads.get(), "a cache hit should not query the database");
        cache.invalidate(new PublicMenuChanged());
        assertEquals("New", cache.get(() -> { loads.incrementAndGet(); return menu("New"); }).store().name());
        assertEquals(2, loads.get());

        data.clear();
        assertEquals("Old", cache.get(() -> {
            cache.invalidate(new PublicMenuChanged()); // A writer commits during this database read.
            return menu("Old");
        }).store().name());
        assertTrue(data.containsKey(DATA + "0"));
        assertEquals("Fresh", cache.get(() -> menu("Fresh")).store().name(),
                "a stale in-flight read must not fill the new revision");
    }

    @SuppressWarnings("unchecked")
    @Test
    void missingRedisAndMalformedCacheFallBackToDatabase() {
        ObjectProvider<StringRedisTemplate> provider = mock(ObjectProvider.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(provider.getIfAvailable()).thenReturn(redis);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get(REVISION)).thenReturn(null);
        when(values.get(DATA + "0")).thenReturn("null");
        PublicMenuCache cache = new PublicMenuCache(true, provider, new ObjectMapper(), "sambar");
        assertEquals("Database", cache.get(() -> menu("Database")).store().name());
        verify(values).set(eq(DATA + "0"), anyString(), eq(Duration.ofSeconds(30)));

        when(values.get(REVISION)).thenThrow(new IllegalStateException("Redis unavailable"));
        assertEquals("Database", cache.get(() -> menu("Database")).store().name());
        assertDoesNotThrow(() -> cache.invalidate(new PublicMenuChanged()));
        PublicMenuCache disabled = new PublicMenuCache(false, provider, new ObjectMapper(), "sambar");
        assertEquals("Database", disabled.get(() -> menu("Database")).store().name());
    }
}
