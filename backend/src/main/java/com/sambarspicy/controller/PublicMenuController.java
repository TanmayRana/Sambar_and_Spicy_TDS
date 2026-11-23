package com.sambarspicy.controller;

import com.sambarspicy.service.PublicMenuService;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
public class PublicMenuController {
    private final PublicMenuService service;

    public PublicMenuController(PublicMenuService service) {
        this.service = service;
    }

    public record Store(String name, String address, String phone) {}
    public record PublicCategory(Long id, String name, String imagePath) {}
    public record PublicItem(Long id, Long categoryId, String name, String description,
                             String dietaryType, BigDecimal sellingPrice, boolean hasHalfPortion,
                             BigDecimal halfSellingPrice, String imagePath) {}
    public record PublicMenu(Store store, List<PublicCategory> categories, List<PublicItem> items) {}

    @GetMapping("/api/v1/public/menu")
    public ResponseEntity<PublicMenu> menu() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.getMenu());
    }
}
