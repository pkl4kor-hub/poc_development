package com.ecommerce.poc.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "products", indexes = {
    @Index(name = "idx_products_name", columnList = "name"),
    @Index(name = "idx_products_category", columnList = "category"),
    @Index(name = "idx_products_seller_id", columnList = "seller_id"),
    @Index(name = "idx_products_active", columnList = "active")
})
public class Product {
    @Id
    @Column(nullable = false)
    private String id;

    @Column(nullable = false)
    private String sku;

    @Column(nullable = false)
    private String name;

    @Column
    private String subtitle;

    @Column
    private String brand = "Bosch";

    @Column(name = "featured")
    private Boolean featured = false;

    @Column(nullable = false)
    private String category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "stock_quantity", nullable = false)
    private int stockQuantity;

    @Column(name = "image_url", nullable = false)
    private String imageUrl;

    @Column(name = "seller_id", nullable = false)
    private Long sellerId;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at")
    private String createdAt = Instant.now().toString();

    @Column(name = "updated_at")
    private String updatedAt = Instant.now().toString();

    @Column(columnDefinition = "TEXT")
    private String specs;

    @PrePersist
    public void onCreate() {
        String now = Instant.now().toString();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = Instant.now().toString();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSku() { return sku; }
    public void setSku(String sku) { this.sku = sku; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }
    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }
    public boolean isFeatured() { return Boolean.TRUE.equals(featured); }
    public Boolean getFeatured() { return featured; }
    public void setFeatured(Boolean featured) { this.featured = featured; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStockQuantity() { return stockQuantity; }
    public void setStockQuantity(int stockQuantity) { this.stockQuantity = stockQuantity; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
}
