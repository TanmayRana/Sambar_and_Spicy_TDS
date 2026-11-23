package com.sambarspicy.service;

import com.sambarspicy.dto.CategoryDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sambarspicy.dto.CategoryRequest;
import com.sambarspicy.exception.ConflictException;
import com.sambarspicy.exception.DuplicateResourceException;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.Category;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final FileStorageService fileStorageService;
    private final ImageCleanupService imageCleanupService;
    private final ApplicationEventPublisher events;
    private final PublicMenuCache cache;
    private static final TypeReference<List<CategoryDto>> CATEGORY_LIST = new TypeReference<>() {};

    public CategoryService(CategoryRepository categoryRepository,
                           MenuItemRepository menuItemRepository,
                           FileStorageService fileStorageService,
                           ImageCleanupService imageCleanupService,
                           ApplicationEventPublisher events, PublicMenuCache cache) {
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.fileStorageService = fileStorageService;
        this.imageCleanupService = imageCleanupService;
        this.events = events;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getActiveCategories() {
        return cache.getCatalog("categories-active", CATEGORY_LIST, () -> categoryRepository.findByIsActiveTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public List<CategoryDto> getAllCategories() {
        return cache.getCatalog("categories-all", CATEGORY_LIST, () -> categoryRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList()));
    }

    @Transactional(readOnly = true)
    public CategoryDto getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        return toDto(category);
    }

    @Transactional
    public CategoryDto createCategory(CategoryRequest request) {
        if (request.getName() == null || request.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Category name is required");
        }
        String trimmedName = request.getName().trim();

        if (categoryRepository.existsByNameIgnoreCase(trimmedName)) {
            throw new DuplicateResourceException("Category name already exists: " + trimmedName);
        }

        Category category = new Category();
        category.setName(trimmedName);
        category.setImagePath(request.getImagePath());
        category.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        category.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        Category saved = categoryRepository.save(category);
        events.publishEvent(new PublicMenuChanged());
        return toDto(saved);
    }

    @Transactional
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        Category category = categoryRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));
        String previousImage = category.getImagePath();

        if (request.getName() != null && request.getName().isBlank()) {
            throw new IllegalArgumentException("Category name is required");
        }
        if (request.getName() != null) {
            String trimmedName = request.getName().trim();
            if (categoryRepository.existsByNameIgnoreCaseAndIdNot(trimmedName, id)) {
                throw new DuplicateResourceException("Another category already exists with name: " + trimmedName);
            }
            category.setName(trimmedName);
        }

        if (request.isImagePathPresent()) {
            category.setImagePath(request.getImagePath());
        }

        if (request.getSortOrder() != null) {
            category.setSortOrder(request.getSortOrder());
        }

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        Category updated = categoryRepository.save(category);
        imageCleanupService.afterChange(previousImage, updated.getImagePath(), false);
        events.publishEvent(new PublicMenuChanged());
        return toDto(updated);
    }

    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        if (menuItemRepository.existsByCategoryId(id)) {
            throw new ConflictException("Cannot delete category '" + category.getName() +
                    "' because it contains associated menu items. Reassign or delete the items first.");
        }

        categoryRepository.delete(category);
        imageCleanupService.afterChange(category.getImagePath(), null, false);
        events.publishEvent(new PublicMenuChanged());
    }

    @Transactional
    public CategoryDto uploadImage(Long id, MultipartFile file) {
        Category category = categoryRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + id));

        String imagePath = fileStorageService.storeFile(file, "cat-" + id);
        imageCleanupService.afterChange(category.getImagePath(), imagePath, true);
        category.setImagePath(imagePath);
        Category saved = categoryRepository.save(category);
        events.publishEvent(new PublicMenuChanged());
        return toDto(saved);
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getImagePath(),
                category.getSortOrder(),
                category.getIsActive(),
                category.getCreatedAt(),
                category.getUpdatedAt()
        );
    }
}
