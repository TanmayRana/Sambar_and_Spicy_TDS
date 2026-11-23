package com.sambarspicy;

import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.config.RedisCacheConfiguration;
import com.sambarspicy.controller.PublicMenuController.PublicMenu;
import com.sambarspicy.controller.PublicMenuController.Store;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import com.sambarspicy.repository.StoreSettingRepository;
import com.sambarspicy.service.CategoryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Run only against a disposable Redis-compatible instance created for this test.
@EnabledIfEnvironmentVariable(named = "TEST_REDIS_URL", matches = ".+")
@SpringBootTest(properties = {
        "app.public-menu-cache.enabled=true",
        "app.spring-cache.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:realredis_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PublicMenuRedisIntegrationTest {
    private static final String NAMESPACE = "sambar-test-" + UUID.randomUUID();
    private static final String REVISION = NAMESPACE + ":public-menu:v1:revision";
    private static final String DATA = NAMESPACE + ":public-menu:v1:data:";
    private static final String SPRING_KEY = NAMESPACE + ":spring-cache:v1:public-menu-snapshot::sample";
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url", () -> System.getenv("TEST_REDIS_URL"));
        registry.add("app.public-menu-cache.namespace", () -> NAMESPACE);
    }

    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired MenuItemRepository items;
    @Autowired StoreSettingRepository stores;
    @Autowired CategoryService categoryService;
    @Autowired StringRedisTemplate redis;
    @Autowired CacheManager cacheManager;

    @Test
    void preparedSpringCacheUsesTypedJsonTtlAndIsolatedKeys() {
        Cache cache = cacheManager.getCache(RedisCacheConfiguration.PUBLIC_MENU_SNAPSHOT);
        assertNotNull(cache);
        assertNull(cacheManager.getCache("unconfigured"));
        PublicMenu menu = new PublicMenu(new Store("Snapshot", "", ""), List.of(), List.of());
        cache.put("sample", menu);
        assertEquals(menu, cache.get("sample", PublicMenu.class));
        String raw = redis.opsForValue().get(SPRING_KEY);
        assertNotNull(raw);
        assertTrue(raw.contains("Snapshot"));
        assertFalse(raw.contains("@class"));
        Long ttl = redis.getExpire(SPRING_KEY, TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 30);
        assertThrows(IllegalArgumentException.class, () -> cache.put("null", null));
    }

    @Test
    void servesCachedPublicMenuAndRevalidatesAfterCommit() throws Exception {
        if (!stores.existsById(1)) stores.saveAndFlush(new StoreSetting("Cache test", "", ""));
        Category category = new Category();
        category.setName("Real Redis category");
        category = categories.saveAndFlush(category);
        items.saveAndFlush(new MenuItem(category, "Real Redis dish", new BigDecimal("80.00")));

        mvc.perform(get("/api/v1/public/menu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", hasItem("Real Redis dish")));
        assertNotNull(redis.opsForValue().get(DATA + "0"));
        Long ttl = redis.getExpire(DATA + "0", TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 30, "the shared menu must expire");
        mvc.perform(get("/api/v1/public/menu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", hasItem("Real Redis dish")));

        CategoryRequest archive = new CategoryRequest();
        archive.setIsActive(false);
        categoryService.updateCategory(category.getId(), archive);
        assertEquals("1", redis.opsForValue().get(REVISION));
        mvc.perform(get("/api/v1/public/menu"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.items[*].name", not(hasItem("Real Redis dish"))));
        assertNotNull(redis.opsForValue().get(DATA + "1"));
    }

    @AfterAll
    void cleanOwnKeys() { redis.delete(List.of(REVISION, DATA + "0", DATA + "1", SPRING_KEY)); }
}
