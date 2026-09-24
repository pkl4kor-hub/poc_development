package com.ecommerce.poc.dto;

import java.math.BigDecimal;
import java.util.Map;

public class ProductResponse {
    private String id;
    private String sku;
    private String name;
    private String subtitle;
    private String brand;
    private String category;
    private BigDecimal price;
    private int stock;
    private boolean featured;
    private String image;
    private String description;
    private Map<String, String> specs;

    public ProductResponse() {}

    public ProductResponse(String id, String sku, String name, String subtitle, String brand, String category, BigDecimal price, int stock, boolean featured, String image, String description, Map<String, String> specs) {
        this.id = id;
        this.sku = sku;
        this.name = name;
        this.subtitle = subtitle;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.stock = stock;
        this.featured = featured;
        this.image = image;
        this.description = description;
        this.specs = specs;
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
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public boolean isFeatured() { return featured; }
    public void setFeatured(boolean featured) { this.featured = featured; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Map<String, String> getSpecs() { return specs; }
    public void setSpecs(Map<String, String> specs) { this.specs = specs; }
}
