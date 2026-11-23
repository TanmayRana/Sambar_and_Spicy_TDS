package com.sambarspicy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public class MenuItemRequest {
    @JsonIgnore private boolean skuPresent;
    @JsonIgnore private boolean descriptionPresent;
    @JsonIgnore private boolean estimatedUnitCostPresent;
    @JsonIgnore private boolean halfEstimatedUnitCostPresent;
    @JsonIgnore private boolean imagePathPresent;

    @JsonProperty("category_id")
    @JsonAlias("categoryId")
    private Long categoryId;

    @Size(max = 50, message = "SKU must not exceed 50 characters")
    private String sku;

    @Size(max = 150, message = "Dish name must not exceed 150 characters")
    private String name;

    @Size(max = 300, message = "Description must not exceed 300 characters")
    private String description;

    @JsonProperty("selling_price")
    @JsonAlias("sellingPrice")
    @DecimalMin(value = "0.0", inclusive = true, message = "Selling price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Selling price must have at most 2 decimal places")
    private BigDecimal sellingPrice;

    @JsonProperty("has_half_portion")
    @JsonAlias("hasHalfPortion")
    private Boolean hasHalfPortion;

    @JsonProperty("half_selling_price")
    @JsonAlias("halfSellingPrice")
    @DecimalMin(value = "0.0", inclusive = true, message = "Half selling price must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Half selling price must have at most 2 decimal places")
    private BigDecimal halfSellingPrice;

    @JsonProperty("estimated_unit_cost")
    @JsonAlias("estimatedUnitCost")
    @DecimalMin(value = "0.0", inclusive = true, message = "Estimated unit cost must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Estimated unit cost must have at most 2 decimal places")
    private BigDecimal estimatedUnitCost;

    @JsonProperty("half_estimated_unit_cost")
    @JsonAlias("halfEstimatedUnitCost")
    @DecimalMin(value = "0.0", inclusive = true, message = "Half estimated unit cost must be non-negative")
    @Digits(integer = 10, fraction = 2, message = "Half estimated unit cost must have at most 2 decimal places")
    private BigDecimal halfEstimatedUnitCost;

    @JsonProperty("image_path")
    @JsonAlias("imagePath")
    @Size(max = 512, message = "Image path must not exceed 512 characters")
    private String imagePath;

    @JsonProperty("sort_order")
    @JsonAlias("sortOrder")
    private Integer sortOrder;

    @JsonProperty("is_available")
    @JsonAlias("isAvailable")
    private Boolean isAvailable;

    @JsonProperty("dietary_type")
    @JsonAlias("dietaryType")
    private String dietaryType;

    public MenuItemRequest() {
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
        this.skuPresent = true;
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
        this.descriptionPresent = true;
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
        this.estimatedUnitCostPresent = true;
    }

    public BigDecimal getHalfEstimatedUnitCost() {
        return halfEstimatedUnitCost;
    }

    public void setHalfEstimatedUnitCost(BigDecimal halfEstimatedUnitCost) {
        this.halfEstimatedUnitCost = halfEstimatedUnitCost;
        this.halfEstimatedUnitCostPresent = true;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.imagePathPresent = true;
    }

    @JsonIgnore public boolean isSkuPresent() { return skuPresent; }
    @JsonIgnore public boolean isDescriptionPresent() { return descriptionPresent; }
    @JsonIgnore public boolean isEstimatedUnitCostPresent() { return estimatedUnitCostPresent; }
    @JsonIgnore public boolean isHalfEstimatedUnitCostPresent() { return halfEstimatedUnitCostPresent; }
    @JsonIgnore public boolean isImagePathPresent() { return imagePathPresent; }

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
        this.dietaryType = dietaryType;
    }
}
