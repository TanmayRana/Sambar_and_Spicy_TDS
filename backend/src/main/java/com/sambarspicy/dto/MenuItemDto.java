package com.sambarspicy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class MenuItemDto {

    private Long id;

    @JsonProperty("category_id")
    @JsonAlias("categoryId")
    private Long categoryId;

    @JsonProperty("category_name")
    @JsonAlias("categoryName")
    private String categoryName;

    private String sku;
    private String name;
    private String description;

    @JsonProperty("selling_price")
    @JsonAlias("sellingPrice")
    private BigDecimal sellingPrice;

    @JsonProperty("has_half_portion")
    @JsonAlias("hasHalfPortion")
    private Boolean hasHalfPortion;

    @JsonProperty("half_selling_price")
    @JsonAlias("halfSellingPrice")
    private BigDecimal halfSellingPrice;

    @JsonProperty("estimated_unit_cost")
    @JsonAlias("estimatedUnitCost")
    private BigDecimal estimatedUnitCost;

    @JsonProperty("half_estimated_unit_cost")
    @JsonAlias("halfEstimatedUnitCost")
    private BigDecimal halfEstimatedUnitCost;

    @JsonProperty("image_path")
    @JsonAlias("imagePath")
    private String imagePath;

    @JsonProperty("sort_order")
    @JsonAlias("sortOrder")
    private Integer sortOrder;

    @JsonProperty("is_available")
    @JsonAlias("isAvailable")
    private Boolean isAvailable;

    @JsonProperty("dietary_type")
    @JsonAlias("dietaryType")
    private String dietaryType = "UNSPECIFIED";

    @JsonProperty("created_at")
    @JsonAlias("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updated_at")
    @JsonAlias("updatedAt")
    private LocalDateTime updatedAt;

    public MenuItemDto() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
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

    public BigDecimal getSellingPrice() {
        return sellingPrice;
    }

    public void setSellingPrice(BigDecimal sellingPrice) {
        this.sellingPrice = sellingPrice;
    }

    public Boolean getHasHalfPortion() {
        return hasHalfPortion;
    }

    public void setHasHalfPortion(Boolean hasHalfPortion) {
        this.hasHalfPortion = hasHalfPortion;
    }

    public BigDecimal getHalfSellingPrice() {
        return halfSellingPrice;
    }

    public void setHalfSellingPrice(BigDecimal halfSellingPrice) {
        this.halfSellingPrice = halfSellingPrice;
    }

    public BigDecimal getEstimatedUnitCost() {
        return estimatedUnitCost;
    }

    public void setEstimatedUnitCost(BigDecimal estimatedUnitCost) {
        this.estimatedUnitCost = estimatedUnitCost;
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

    public String getDietaryType() {
        return dietaryType;
    }

    public void setDietaryType(String dietaryType) {
        this.dietaryType = dietaryType != null ? dietaryType : "UNSPECIFIED";
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
}
