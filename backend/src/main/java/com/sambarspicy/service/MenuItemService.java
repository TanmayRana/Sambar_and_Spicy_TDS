package com.sambarspicy.service;

import com.sambarspicy.dto.MenuItemDto;
import com.fasterxml.jackson.core.type.TypeReference;
import com.sambarspicy.dto.MenuItemRequest;
import com.sambarspicy.enums.DietaryType;
import com.sambarspicy.exception.ConflictException;
import com.sambarspicy.exception.DuplicateResourceException;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.MenuItem;
import com.sambarspicy.repository.BillItemRepository;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final BillItemRepository billItemRepository;
    private final FileStorageService fileStorageService;
    private final ImageCleanupService imageCleanupService;
    private final ApplicationEventPublisher events;
    private final PublicMenuCache cache;
    private static final TypeReference<List<MenuItemDto>> MENU_LIST = new TypeReference<>() {};

    public MenuItemService(MenuItemRepository menuItemRepository,
                           CategoryRepository categoryRepository,
                           BillItemRepository billItemRepository,
                           FileStorageService fileStorageService,
                           ImageCleanupService imageCleanupService,
                           ApplicationEventPublisher events, PublicMenuCache cache) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.billItemRepository = billItemRepository;
        this.fileStorageService = fileStorageService;
        this.imageCleanupService = imageCleanupService;
        this.events = events;
        this.cache = cache;
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> getActiveMenuItems() {
        return menuItemRepository.findByIsAvailableTrueOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> getAllMenuItems() {
        return menuItemRepository.findAllByOrderBySortOrderAsc()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> getMenuItemsByCategory(Long categoryId, boolean activeOnly) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
        }
        List<MenuItem> items = activeOnly
                ? menuItemRepository.findByCategoryIdAndIsAvailableTrueOrderBySortOrderAsc(categoryId)
                : menuItemRepository.findByCategoryIdOrderBySortOrderAsc(categoryId);
        return items.stream().map(this::toDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public MenuItemDto getMenuItemById(Long id) {
        MenuItem item = menuItemRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with ID: " + id));
        return toDto(item);
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> searchMenuItems(String query, boolean activeOnly) {
        return menuItemRepository.searchMenuItems(null, query, activeOnly)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MenuItemDto> getFilteredMenuItems(Long categoryId, String search, boolean activeOnly) {
        if (categoryId != null && !categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category not found with ID: " + categoryId);
        }
        if (categoryId == null && !StringUtils.hasText(search)) {
            return cache.getCatalog(activeOnly ? "items-active" : "items-all", MENU_LIST,
                    () -> menuItemRepository.searchMenuItems(null, search, activeOnly)
                            .stream().map(this::toDto).collect(Collectors.toList()));
        }
        return menuItemRepository.searchMenuItems(categoryId, search, activeOnly)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public MenuItemDto createMenuItem(MenuItemRequest request) {
        if (request.getCategoryId() == null) {
            throw new IllegalArgumentException("Category ID is required");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Dish name is required");
        }
        if (request.getSellingPrice() == null) {
            throw new IllegalArgumentException("Selling price is required");
        }
        if (request.getSellingPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Selling price must be non-negative");
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));

        String trimmedName = request.getName().trim();
        if (menuItemRepository.existsByCategoryIdAndNameIgnoreCase(category.getId(), trimmedName)) {
            throw new DuplicateResourceException("A dish named '" + trimmedName + "' already exists in category '" + category.getName() + "'");
        }

        String trimmedSku = request.getSku() != null && !request.getSku().isBlank() ? request.getSku().trim() : null;
        if (trimmedSku != null && menuItemRepository.existsBySkuIgnoreCase(trimmedSku)) {
            throw new DuplicateResourceException("SKU already exists: " + trimmedSku);
        }

        boolean hasHalf = Boolean.TRUE.equals(request.getHasHalfPortion());
        if (hasHalf) {
            if (request.getHalfSellingPrice() == null || request.getHalfSellingPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Half selling price is required and must be non-negative when half portion is enabled");
            }
        }

        if (request.getEstimatedUnitCost() != null && request.getEstimatedUnitCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Estimated unit cost must be non-negative");
        }
        if (request.getHalfEstimatedUnitCost() != null && request.getHalfEstimatedUnitCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Half estimated unit cost must be non-negative");
        }

        MenuItem menuItem = new MenuItem();
        menuItem.setCategory(category);
        menuItem.setName(trimmedName);
        menuItem.setSku(trimmedSku);
        menuItem.setDescription(request.getDescription() != null && !request.getDescription().isBlank() ? request.getDescription().trim() : null);
        if (request.getDietaryType() != null) {
            menuItem.setDietaryType(parseDietaryType(request.getDietaryType()));
        }
        menuItem.setSellingPrice(request.getSellingPrice());
        menuItem.setHasHalfPortion(hasHalf);
        menuItem.setHalfSellingPrice(hasHalf ? request.getHalfSellingPrice() : null);
        menuItem.setEstimatedUnitCost(request.getEstimatedUnitCost());
        menuItem.setHalfEstimatedUnitCost(hasHalf ? request.getHalfEstimatedUnitCost() : null);
        menuItem.setImagePath(request.getImagePath());
        menuItem.setSortOrder(request.getSortOrder() != null ? request.getSortOrder() : 0);
        menuItem.setIsAvailable(request.getIsAvailable() != null ? request.getIsAvailable() : true);

        MenuItem saved = menuItemRepository.save(menuItem);
        events.publishEvent(new PublicMenuChanged());
        return toDto(saved);
    }

    @Transactional
    public MenuItemDto updateMenuItem(Long id, MenuItemRequest request) {
        MenuItem menuItem = menuItemRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with ID: " + id));
        String previousImage = menuItem.getImagePath();

        if (request.getCategoryId() != null && !request.getCategoryId().equals(menuItem.getCategory().getId())) {
            Category newCategory = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category not found with ID: " + request.getCategoryId()));
            menuItem.setCategory(newCategory);
        }

        if (request.getName() != null && request.getName().isBlank()) {
            throw new IllegalArgumentException("Dish name is required");
        }
        if (request.getCategoryId() != null && request.getName() == null &&
                menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(menuItem.getCategory().getId(), menuItem.getName(), id)) {
            throw new DuplicateResourceException("A dish with this name already exists in the category");
        }
        if (StringUtils.hasText(request.getName())) {
            String trimmedName = request.getName().trim();
            if (menuItemRepository.existsByCategoryIdAndNameIgnoreCaseAndIdNot(menuItem.getCategory().getId(), trimmedName, id)) {
                throw new DuplicateResourceException("A dish named '" + trimmedName + "' already exists in category '" + menuItem.getCategory().getName() + "'");
            }
            menuItem.setName(trimmedName);
        }

        if (request.getDietaryType() != null) {
            menuItem.setDietaryType(parseDietaryType(request.getDietaryType()));
        }

        if (request.isSkuPresent()) {
            String trimmedSku = request.getSku() == null || request.getSku().isBlank() ? null : request.getSku().trim();
            if (trimmedSku != null && menuItemRepository.existsBySkuIgnoreCaseAndIdNot(trimmedSku, id)) {
                throw new DuplicateResourceException("Another dish already has SKU: " + trimmedSku);
            }
            menuItem.setSku(trimmedSku);
        }

        if (request.isDescriptionPresent()) {
            menuItem.setDescription(request.getDescription() == null || request.getDescription().isBlank() ? null : request.getDescription().trim());
        }

        if (request.getSellingPrice() != null) {
            if (request.getSellingPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Selling price must be non-negative");
            }
            menuItem.setSellingPrice(request.getSellingPrice());
        }

        if (request.getHasHalfPortion() != null) {
            menuItem.setHasHalfPortion(request.getHasHalfPortion());
            if (!request.getHasHalfPortion()) {
                menuItem.setHalfSellingPrice(null);
                menuItem.setHalfEstimatedUnitCost(null);
            }
        }

        if (request.getHalfSellingPrice() != null) {
            if (request.getHalfSellingPrice().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Half selling price must be non-negative");
            }
            menuItem.setHalfSellingPrice(request.getHalfSellingPrice());
        }

        if (Boolean.TRUE.equals(menuItem.getHasHalfPortion()) && menuItem.getHalfSellingPrice() == null) {
            throw new IllegalArgumentException("Half selling price is required when half portion is enabled");
        }

        if (request.isEstimatedUnitCostPresent()) {
            if (request.getEstimatedUnitCost() != null && request.getEstimatedUnitCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Estimated unit cost must be non-negative");
            }
            menuItem.setEstimatedUnitCost(request.getEstimatedUnitCost());
        }

        if (request.isHalfEstimatedUnitCostPresent()) {
            if (request.getHalfEstimatedUnitCost() != null && request.getHalfEstimatedUnitCost().compareTo(java.math.BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Half estimated unit cost must be non-negative");
            }
            if (Boolean.TRUE.equals(menuItem.getHasHalfPortion())) {
                menuItem.setHalfEstimatedUnitCost(request.getHalfEstimatedUnitCost());
            }
        }

        if (request.isImagePathPresent()) {
            menuItem.setImagePath(request.getImagePath());
        }

        if (request.getSortOrder() != null) {
            menuItem.setSortOrder(request.getSortOrder());
        }

        if (request.getIsAvailable() != null) {
            menuItem.setIsAvailable(request.getIsAvailable());
        }

        MenuItem updated = menuItemRepository.save(menuItem);
        imageCleanupService.afterChange(previousImage, updated.getImagePath(), false);
        events.publishEvent(new PublicMenuChanged());
        return toDto(updated);
    }

    @Transactional
    public void deleteMenuItem(Long id) {
        MenuItem menuItem = menuItemRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with ID: " + id));

        if (billItemRepository.existsByMenuItemId(id)) {
            throw new ConflictException("Cannot delete menu item '" + menuItem.getName() +
                    "' because it is referenced in past bills. Please mark it as unavailable instead.");
        }

        menuItemRepository.delete(menuItem);
        imageCleanupService.afterChange(menuItem.getImagePath(), null, false);
        events.publishEvent(new PublicMenuChanged());
    }

    @Transactional
    public MenuItemDto uploadImage(Long id, MultipartFile file) {
        MenuItem menuItem = menuItemRepository.findByIdForBilling(id)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with ID: " + id));

        String imagePath = fileStorageService.storeFile(file, "dish-" + id);
        imageCleanupService.afterChange(menuItem.getImagePath(), imagePath, true);
        menuItem.setImagePath(imagePath);
        MenuItem saved = menuItemRepository.save(menuItem);
        events.publishEvent(new PublicMenuChanged());
        return toDto(saved);
    }

    public MenuItemDto toDto(MenuItem item) {
        MenuItemDto dto = new MenuItemDto();
        dto.setId(item.getId());
        dto.setCategoryId(item.getCategory() != null ? item.getCategory().getId() : null);
        dto.setCategoryName(item.getCategory() != null ? item.getCategory().getName() : null);
        dto.setSku(item.getSku());
        dto.setName(item.getName());
        dto.setDescription(item.getDescription());
        dto.setSellingPrice(item.getSellingPrice());
        dto.setHasHalfPortion(item.getHasHalfPortion());
        dto.setHalfSellingPrice(item.getHalfSellingPrice());
        dto.setEstimatedUnitCost(item.getEstimatedUnitCost());
        dto.setHalfEstimatedUnitCost(item.getHalfEstimatedUnitCost());
        dto.setImagePath(item.getImagePath());
        dto.setSortOrder(item.getSortOrder());
        dto.setIsAvailable(item.getIsAvailable());
        dto.setDietaryType(item.getDietaryType().name());
        dto.setCreatedAt(item.getCreatedAt());
        dto.setUpdatedAt(item.getUpdatedAt());
        return dto;
    }

    private DietaryType parseDietaryType(String value) {
        try {
            return DietaryType.valueOf(value);
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid dietary type. Choose UNSPECIFIED, VEG, NON_VEG, or EGG.");
        }
    }
}
