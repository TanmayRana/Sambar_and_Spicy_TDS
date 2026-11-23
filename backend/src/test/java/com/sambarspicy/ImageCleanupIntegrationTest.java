package com.sambarspicy;

import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.dto.MenuItemRequest;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import com.sambarspicy.service.CategoryService;
import com.sambarspicy.service.FileStorageService;
import com.sambarspicy.service.MenuItemService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
class ImageCleanupIntegrationTest {
    private static final Path UPLOADS = temporaryDirectory();
    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("app.upload.dir", UPLOADS::toString);
    }
    private static Path temporaryDirectory() {
        try { return Files.createTempDirectory("sambar-image-test-"); }
        catch (IOException e) { throw new RuntimeException(e); }
    }

    @Autowired CategoryRepository categories;
    @Autowired MenuItemRepository menuItems;
    @Autowired CategoryService categoryService;
    @Autowired MenuItemService menuItemService;
    @Autowired FileStorageService storage;
    @Autowired PlatformTransactionManager transactionManager;

    private MockMultipartFile image() {
        return new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 0x50, 0x4e, 0x47, 0x0d, 0x0a, 0x1a, 0x0a, 1, 2});
    }
    private Path file(String path) { return UPLOADS.resolve(path.substring("/uploads/".length())); }

    @Test
    void categoryAndDishReplacementRemovalAndSharedReferences() {
        Category category = new Category();
        category.setName("Image cleanup category");
        category = categories.saveAndFlush(category);
        Long categoryId = category.getId();
        MenuItem item = menuItems.saveAndFlush(new MenuItem(category, "Image cleanup dish", BigDecimal.TEN));
        Long itemId = item.getId();

        String firstCategory = categoryService.uploadImage(categoryId, image()).getImagePath();
        String secondCategory = categoryService.uploadImage(categoryId, image()).getImagePath();
        assertFalse(Files.exists(file(firstCategory)));
        assertTrue(Files.exists(file(secondCategory)));

        String firstDish = menuItemService.uploadImage(itemId, image()).getImagePath();
        String secondDish = menuItemService.uploadImage(itemId, image()).getImagePath();
        assertFalse(Files.exists(file(firstDish)));
        assertTrue(Files.exists(file(secondDish)));

        MenuItemRequest shared = new MenuItemRequest();
        shared.setImagePath(secondCategory);
        menuItemService.updateMenuItem(itemId, shared);
        assertFalse(Files.exists(file(secondDish)));
        CategoryRequest removeCategory = new CategoryRequest();
        removeCategory.setImagePath(null);
        categoryService.updateCategory(categoryId, removeCategory);
        assertTrue(Files.exists(file(secondCategory)), "shared images must remain while referenced");
        MenuItemRequest removeDish = new MenuItemRequest();
        removeDish.setImagePath(null);
        menuItemService.updateMenuItem(itemId, removeDish);
        assertFalse(Files.exists(file(secondCategory)), "last removal should clean the shared file");
    }

    @Test
    void rollbackPreservesOldFileAndCleansFailedUpload() throws IOException {
        Category category = new Category();
        category.setName("Image rollback category");
        Long id = categories.saveAndFlush(category).getId();
        String original = categoryService.uploadImage(id, image()).getImagePath();
        long before;
        try (var files = Files.list(UPLOADS)) { before = files.count(); }
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        assertThrows(IllegalStateException.class, () -> transaction.executeWithoutResult(status -> {
            categoryService.uploadImage(id, image());
            throw new IllegalStateException("simulate rollback");
        }));
        assertTrue(Files.exists(file(original)));
        assertEquals(original, categories.findById(id).orElseThrow().getImagePath());
        try (var files = Files.list(UPLOADS)) { assertEquals(before, files.count()); }
        CategoryRequest remove = new CategoryRequest();
        remove.setImagePath(null);
        categoryService.updateCategory(id, remove);
    }

    @Test
    void ignoresUnmanagedAndTraversalPaths() throws IOException {
        Path unmanaged = UPLOADS.resolve("important.png");
        Files.writeString(unmanaged, "keep");
        storage.deleteManagedImage("/uploads/important.png");
        storage.deleteManagedImage("/uploads/../important.png");
        storage.deleteManagedImage("https://example.com/image.png");
        assertTrue(Files.exists(unmanaged));
        Files.delete(unmanaged);
    }

    @AfterAll
    static void removeTestDirectory() throws IOException {
        Files.deleteIfExists(UPLOADS);
    }
}
