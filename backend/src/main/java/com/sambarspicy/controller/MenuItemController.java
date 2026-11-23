package com.sambarspicy.controller;

import com.sambarspicy.dto.MenuItemDto;
import com.sambarspicy.dto.MenuItemRequest;
import com.sambarspicy.service.MenuItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/menu-items", "/api/v1/menu-items", "/api/menu", "/api/v1/menu"})
public class MenuItemController {

    private final MenuItemService menuItemService;

    public MenuItemController(MenuItemService menuItemService) {
        this.menuItemService = menuItemService;
    }

    @GetMapping
    public ResponseEntity<List<MenuItemDto>> getMenuItems(
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "search", required = false) String search,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {

        String searchTerm = search != null ? search : (name != null ? name : q);
        List<MenuItemDto> items = menuItemService.getFilteredMenuItems(categoryId, searchTerm, activeOnly);
        return ResponseEntity.ok(items);
    }

    @GetMapping("/{id}")
    public ResponseEntity<MenuItemDto> getMenuItemById(@PathVariable Long id) {
        return ResponseEntity.ok(menuItemService.getMenuItemById(id));
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<List<MenuItemDto>> getMenuItemsByCategory(
            @PathVariable Long categoryId,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        return ResponseEntity.ok(menuItemService.getMenuItemsByCategory(categoryId, activeOnly));
    }

    @GetMapping("/search")
    public ResponseEntity<List<MenuItemDto>> searchMenuItems(
            @RequestParam(name = "name", required = false) String name,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "activeOnly", required = false, defaultValue = "false") boolean activeOnly) {
        String query = name != null ? name : q;
        return ResponseEntity.ok(menuItemService.searchMenuItems(query, activeOnly));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuItemDto> createMenuItem(@Valid @RequestBody MenuItemRequest request) {
        MenuItemDto created = menuItemService.createMenuItem(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuItemDto> updateMenuItem(@PathVariable Long id,
                                                      @Valid @RequestBody MenuItemRequest request) {
        MenuItemDto updated = menuItemService.updateMenuItem(id, request);
        return ResponseEntity.ok(updated);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MenuItemDto> patchMenuItem(@PathVariable Long id,
                                                     @Valid @RequestBody MenuItemRequest request) {
        MenuItemDto updated = menuItemService.updateMenuItem(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteMenuItem(@PathVariable Long id) {
        menuItemService.deleteMenuItem(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> uploadImage(@PathVariable Long id,
                                                           @RequestParam("file") MultipartFile file) {
        MenuItemDto updated = menuItemService.uploadImage(id, file);
        return ResponseEntity.ok(Map.of("image_path", updated.getImagePath()));
    }
}
