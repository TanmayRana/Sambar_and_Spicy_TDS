package com.sambarspicy.service;

import com.sambarspicy.controller.PublicMenuController.PublicCategory;
import com.sambarspicy.controller.PublicMenuController.PublicItem;
import com.sambarspicy.controller.PublicMenuController.PublicMenu;
import com.sambarspicy.controller.PublicMenuController.Store;
import com.sambarspicy.exception.ResourceNotFoundException;
import com.sambarspicy.model.Category;
import com.sambarspicy.model.StoreSetting;
import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import com.sambarspicy.repository.StoreSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class PublicMenuService {
    private final CategoryRepository categories;
    private final MenuItemRepository items;
    private final StoreSettingRepository stores;
    private final PublicMenuCache cache;
    private final TransactionTemplate menuTransaction;

    public PublicMenuService(CategoryRepository categories, MenuItemRepository items,
                             StoreSettingRepository stores, PublicMenuCache cache,
                             PlatformTransactionManager transactionManager) {
        this.categories = categories;
        this.items = items;
        this.stores = stores;
        this.cache = cache;
        this.menuTransaction = new TransactionTemplate(transactionManager);
        this.menuTransaction.setReadOnly(true);
    }

    public PublicMenu getMenu() {
        return cache.get(() -> menuTransaction.execute(status -> {
            System.out.println(">>> [DATABASE HIT] Loading menu from MySQL Database...");
            return loadMenu();
        }));
    }

    private PublicMenu loadMenu() {
        StoreSetting setting = stores.findById(1).orElseThrow(() -> new ResourceNotFoundException("Store settings not found"));
        List<Category> activeCategories = categories.findByIsActiveTrueOrderBySortOrderAsc();
        Set<Long> ids = activeCategories.stream().map(Category::getId).collect(Collectors.toSet());
        List<PublicItem> available = items.findByIsAvailableTrueOrderBySortOrderAsc().stream()
                .filter(item -> ids.contains(item.getCategory().getId()))
                .map(item -> new PublicItem(item.getId(), item.getCategory().getId(), item.getName(),
                        item.getDescription(), item.getDietaryType().name(), item.getSellingPrice(),
                        Boolean.TRUE.equals(item.getHasHalfPortion()), item.getHalfSellingPrice(), item.getImagePath()))
                .toList();
        Set<Long> used = available.stream().map(PublicItem::categoryId).collect(Collectors.toSet());
        List<PublicCategory> publicCategories = activeCategories.stream().filter(c -> used.contains(c.getId()))
                .map(c -> new PublicCategory(c.getId(), c.getName(), c.getImagePath())).toList();
        return new PublicMenu(new Store(setting.getStoreName(), setting.getAddress(), setting.getPhone()),
                publicCategories, available);
    }
}
