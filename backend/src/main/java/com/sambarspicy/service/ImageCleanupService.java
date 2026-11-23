package com.sambarspicy.service;

import com.sambarspicy.repository.CategoryRepository;
import com.sambarspicy.repository.MenuItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Objects;

@Service
public class ImageCleanupService {
    private static final Logger log = LoggerFactory.getLogger(ImageCleanupService.class);
    private final FileStorageService storage;
    private final CategoryRepository categories;
    private final MenuItemRepository menuItems;
    private final TransactionTemplate readTransaction;

    public ImageCleanupService(FileStorageService storage, CategoryRepository categories,
                               MenuItemRepository menuItems, PlatformTransactionManager transactionManager) {
        this.storage = storage;
        this.categories = categories;
        this.menuItems = menuItems;
        this.readTransaction = new TransactionTemplate(transactionManager);
        this.readTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.readTransaction.setReadOnly(true);
    }

    public void afterChange(String previous, String replacement, boolean replacementIsNewFile) {
        if (Objects.equals(previous, replacement)) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            throw new IllegalStateException("Image changes require an active transaction");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) deleteIfUnreferenced(previous);
                else if (status == STATUS_ROLLED_BACK && replacementIsNewFile) storage.deleteManagedImage(replacement);
            }
        });
    }

    private void deleteIfUnreferenced(String path) {
        if (path == null) return;
        try {
            Boolean inUse = readTransaction.execute(status ->
                    categories.existsByImagePath(path) || menuItems.existsByImagePath(path));
            if (Boolean.FALSE.equals(inUse)) storage.deleteManagedImage(path);
        } catch (RuntimeException ex) {
            // A failed cleanup must not make a committed save appear to have failed.
            log.warn("Could not check references for obsolete uploaded image {}", path, ex);
        }
    }
}
