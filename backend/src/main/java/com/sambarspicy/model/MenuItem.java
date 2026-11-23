package com.sambarspicy.model;

import com.sambarspicy.enums.DietaryType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * Dish catalog item belonging to a category with selling price and estimated unit cost.
 * Dish images are limited to at most one image path.
 */
@Entity
@Table(
    name = "menu_items",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_menu_item_sku", columnNames = {"sku"}),
        @UniqueConstraint(name = "uq_menu_category_name", columnNames = {"category_id", "name"})
    }
)
public class MenuItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(name = "sku", length = 50)
    private String sku;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Column(name = "description", length = 300)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "dietary_type", nullable = false)
    private DietaryType dietaryType = DietaryType.UNSPECIFIED;

    @Column(name = "selling_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal sellingPrice;

    @Column(name = "has_half_portion", nullable = false)
    private Boolean hasHalfPortion = false;

    @Column(name = "half_selling_price", precision = 12, scale = 2)
    private BigDecimal halfSellingPrice;

    @Column(name = "estimated_unit_cost", precision = 12, scale = 2)
    private BigDecimal estimatedUnitCost;

    @Column(name = "half_estimated_unit_cost", precision = 12, scale = 2)
    private BigDecimal halfEstimatedUnitCost;

    @Column(name = "image_path", length = 512)
    private String imagePath;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder = 0;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public MenuItem() {
    }

    public MenuItem(Category category, String name, BigDecimal sellingPrice) {
        this.category = category;
        this.name = name;
        this.sellingPrice = sellingPrice;
        this.isAvailable = true;
        this.sortOrder = 0;
    }

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.updatedAt == null) {
            this.updatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public DietaryType getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(DietaryType dietaryType) {
        this.dietaryType = Objects.requireNonNull(dietaryType);
    }

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public BigDecimal getEstimatedUnitCost() {
        return estimatedUnitCost;
    }

    public void setEstimatedUnitCost(BigDecimal estimatedUnitCost) {
        this.estimatedUnitCost = estimatedUnitCost;
    }

    public Boolean getHasHalfPortion() {
        return hasHalfPortion;
    }

    public void setHasHalfPortion(Boolean hasHalfPortion) {
        this.hasHalfPortion = hasHalfPortion != null ? hasHalfPortion : false;
    }

    public BigDecimal getHalfSellingPrice() {
        return halfSellingPrice;
    }

    public void setHalfSellingPrice(BigDecimal halfSellingPrice) {
        this.halfSellingPrice = halfSellingPrice;
    }

    public BigDecimal getHalfEstimatedUnitCost() {
        return halfEstimatedUnitCost;
    }

    public void setHalfEstimatedUnitCost(BigDecimal halfEstimatedUnitCost) {
        this.halfEstimatedUnitCost = halfEstimatedUnitCost;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean available) {
        isAvailable = available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MenuItem menuItem = (MenuItem) o;
        return Objects.equals(id, menuItem.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
