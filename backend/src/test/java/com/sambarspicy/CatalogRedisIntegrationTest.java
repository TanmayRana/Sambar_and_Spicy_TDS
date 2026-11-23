package com.sambarspicy;

import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import com.sambarspicy.service.CategoryService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.anonymous;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@EnabledIfEnvironmentVariable(named = "TEST_REDIS_URL", matches = ".+")
@SpringBootTest(properties = {"app.public-menu-cache.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:catalogredis_test;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"})
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CatalogRedisIntegrationTest {
    private static final String PREFIX = "sambar-test-" + UUID.randomUUID();
    private static final String REVISION = PREFIX + ":public-menu:v1:revision";
    private static String key(int revision, String variant) {
        return PREFIX + ":public-menu:v1:data:" + revision + ":catalog:" + variant;
    }
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.url", () -> System.getenv("TEST_REDIS_URL"));
        registry.add("app.public-menu-cache.namespace", () -> PREFIX);
    }

    @Autowired MockMvc mvc;
    @Autowired CategoryRepository categories;
    @Autowired MenuItemRepository items;
    @Autowired CategoryService service;
    @Autowired StringRedisTemplate redis;

    @Test
    @WithMockUser(roles = "ADMIN")
    void cachesCatalogVariantsAndInvalidatesAfterCategoryChange() throws Exception {
        Category category = new Category();
        category.setName("Original category");
        category = categories.saveAndFlush(category);
        items.saveAndFlush(new MenuItem(category, "Test dish", new BigDecimal("80.00")));

        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Original category")));
        mvc.perform(get("/api/v1/menu-items")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category_name", hasItem("Original category")));
        assertNotNull(redis.opsForValue().get(key(0, "categories-all")));
        assertNotNull(redis.opsForValue().get(key(0, "items-all")));
        Long ttl = redis.getExpire(key(0, "items-all"), TimeUnit.SECONDS);
        assertNotNull(ttl);
        assertTrue(ttl > 0 && ttl <= 30);
        // Exercise the cached deserialization path, including private staff DTO fields.
        mvc.perform(get("/api/v1/menu-items")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Test dish")));
        mvc.perform(get("/api/v1/menu-items").param("q", "no matching dish"))
                .andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/v1/menu-items").with(anonymous())).andExpect(status().isUnauthorized());

        CategoryRequest change = new CategoryRequest();
        change.setName("Updated category");
        service.updateCategory(category.getId(), change);
        assertEquals("1", redis.opsForValue().get(REVISION));
        mvc.perform(get("/api/v1/categories")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].name", hasItem("Updated category")));
        mvc.perform(get("/api/v1/menu-items")).andExpect(status().isOk())
                .andExpect(jsonPath("$[*].category_name", hasItem("Updated category")));
        assertNotNull(redis.opsForValue().get(key(1, "items-all")));
    }

    @AfterAll
    void removeOwnKeys() {
        redis.delete(List.of(REVISION, key(0, "categories-all"), key(0, "items-all"),
                key(1, "categories-all"), key(1, "items-all")));
    }
}
