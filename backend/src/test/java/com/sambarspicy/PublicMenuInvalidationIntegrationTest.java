package com.sambarspicy;

import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.dto.MenuItemRequest;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.StoreSettingRepository;
import com.sambarspicy.service.CategoryService;
import com.sambarspicy.service.MenuItemService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "app.public-menu-cache.enabled=true",
        "spring.datasource.url=jdbc:h2:mem:publicmenucache;MODE=MySQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH"
})
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PublicMenuInvalidationIntegrationTest {
    private static final String REVISION = "sambar:public-menu:v1:revision";
    private static final Path UPLOADS = temporaryDirectory();
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) { registry.add("app.upload.dir", UPLOADS::toString); }
    private static Path temporaryDirectory() {
        try { return Files.createTempDirectory("sambar-cache-test-"); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @MockBean StringRedisTemplate redis;
    @Autowired CategoryService categories;
    @Autowired MenuItemService items;
    @Autowired CategoryRepository categoryRepository;
    @Autowired StoreSettingRepository stores;
    @Autowired PlatformTransactionManager manager;
    @Autowired MockMvc mvc;

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2});
    }

    @SuppressWarnings("unchecked")
    @Test
    @WithMockUser(username = "admin", roles = "ADMIN")
    void committedChangesInvalidateAndRollbacksDoNot() throws Exception {
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var category = categories.createCategory(new CategoryRequest("Cache category", null, 0, true));
        verify(values, times(1)).increment(REVISION);
        CategoryRequest edit = new CategoryRequest(); edit.setName("Cache category edited");
        categories.updateCategory(category.getId(), edit);
        categories.uploadImage(category.getId(), image());

        MenuItemRequest dish = new MenuItemRequest();
        dish.setCategoryId(category.getId()); dish.setName("Cache dish"); dish.setSellingPrice(BigDecimal.TEN);
        var item = items.createMenuItem(dish);
        MenuItemRequest unavailable = new MenuItemRequest(); unavailable.setIsAvailable(false);
        items.updateMenuItem(item.getId(), unavailable);
        items.uploadImage(item.getId(), image());
        items.deleteMenuItem(item.getId());
        categories.deleteCategory(category.getId());
        verify(values, times(8)).increment(REVISION);

        new TransactionTemplate(manager).executeWithoutResult(status -> {
            categories.createCategory(new CategoryRequest("Rolled back", null, 0, true));
            status.setRollbackOnly();
        });
        verify(values, times(8)).increment(REVISION);

        if (!stores.existsById(1)) stores.saveAndFlush(new StoreSetting("Test store", "", ""));
        mvc.perform(patch("/api/v1/admin/store").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"store_name\":\"Updated shop\"}")).andExpect(status().isOk());
        verify(values, times(9)).increment(REVISION);

        when(values.increment(REVISION)).thenThrow(new IllegalStateException("Redis offline"));
        var saved = categories.createCategory(new CategoryRequest("Save without Redis", null, 0, true));
        assertTrue(categoryRepository.existsById(saved.getId()), "Redis failure must not undo a committed save");
        categories.deleteCategory(saved.getId());
    }

    @AfterAll
    static void removeTestDirectory() throws IOException { Files.deleteIfExists(UPLOADS); }
}
