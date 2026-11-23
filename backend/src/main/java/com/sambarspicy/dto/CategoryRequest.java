package com.sambarspicy.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Size;

public class CategoryRequest {

    @Size(max = 120, message = "Category name must not exceed 120 characters")
    private String name;

    @JsonProperty("image_path")
    @JsonAlias("imagePath")
    @Size(max = 512, message = "Image path must not exceed 512 characters")
    private String imagePath;
    @JsonIgnore
    private boolean imagePathPresent;

    @JsonProperty("sort_order")
    @JsonAlias("sortOrder")
    private Integer sortOrder;

    @JsonProperty("is_active")
    @JsonAlias("isActive")
    private Boolean isActive;

    public CategoryRequest() {
    }

    public CategoryRequest(String name, String imagePath, Integer sortOrder, Boolean isActive) {
        this.name = name;
        this.imagePath = imagePath;
        this.sortOrder = sortOrder;
        this.isActive = isActive;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
        this.imagePathPresent = true;
    }

    @JsonIgnore
    public boolean isImagePathPresent() { return imagePathPresent; }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean active) {
        isActive = active;
    }
}
