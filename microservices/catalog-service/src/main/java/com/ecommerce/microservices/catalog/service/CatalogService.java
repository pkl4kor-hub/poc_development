package com.ecommerce.microservices.catalog.service;

import com.ecommerce.microservices.catalog.entity.Product;
import com.ecommerce.microservices.catalog.repository.ProductRepository;
import com.ecommerce.microservices.common.dto.ProductResponse;
import com.ecommerce.microservices.common.dto.StockReservationRequest;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CatalogService {

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CatalogService(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public Page<ProductResponse> search(String q, String category, Boolean featured, Boolean inStock,
                                        Integer minPrice, Integer maxPrice, String sort, int page, int size) {
        List<Product> all = productRepository.findByActiveTrueOrderByNameAsc();
        String search = q == null ? "" : q.trim().toLowerCase();
        String cat = category == null ? "" : category.trim();

        List<ProductResponse> filtered = all.stream()
                .filter(p -> {
                    if (!search.isEmpty()) {
                        String combined = (p.getName() + " " + (p.getSubtitle() != null ? p.getSubtitle() : "") + " "
                                + (p.getBrand() != null ? p.getBrand() : "") + " " + p.getDescription() + " "
                                + p.getSku() + " " + p.getCategory()).toLowerCase();
                        if (!combined.contains(search)) return false;
                    }
                    if (!cat.isEmpty() && !cat.equalsIgnoreCase(p.getCategory())) return false;
                    if (Boolean.TRUE.equals(featured) && !p.isFeatured()) return false;
                    if (Boolean.TRUE.equals(inStock) && p.getStockQuantity() <= 0) return false;
                    if (minPrice != null && p.getPrice().intValue() < minPrice) return false;
                    if (maxPrice != null && p.getPrice().intValue() > maxPrice) return false;
                    return true;
                })
                .map(this::toResponse)
                .collect(Collectors.toList());

        Comparator<ProductResponse> comparator = switch (sort == null ? "featured" : sort) {
            case "price-asc" -> Comparator.comparing(ProductResponse::getPrice);
            case "price-desc" -> Comparator.comparing(ProductResponse::getPrice).reversed();
            case "name" -> Comparator.comparing(ProductResponse::getName);
            default -> (a, b) -> {
                int f = Boolean.compare(b.isFeatured(), a.isFeatured());
                return f != 0 ? f : a.getName().compareTo(b.getName());
            };
        };
        filtered.sort(comparator);

        int total = filtered.size();
        int safePage = Math.max(0, page);
        int safeSize = size <= 0 ? 20 : size;
        int start = Math.min(safePage * safeSize, total);
        int end = Math.min(start + safeSize, total);
        List<ProductResponse> paged = filtered.subList(start, end);

        return new PageImpl<>(paged, PageRequest.of(safePage, safeSize), total);
    }

    public Optional<ProductResponse> getProductById(String id) {
        return productRepository.findByIdAndActiveTrue(id).map(this::toResponse);
    }

    public Map<String, Object> getConfig() {
        var page = search("", "", null, null, null, null, "featured", 0, 1000);
        List<Map<String, Object>> shippingMethods = new ArrayList<>();
        shippingMethods.add(new LinkedHashMap<>() {{
            put("id", "standard");
            put("name", "Standard delivery");
            put("estimate", "3-5 business days");
            put("price", 850);
        }});
        shippingMethods.add(new LinkedHashMap<>() {{
            put("id", "express");
            put("name", "Express delivery");
            put("estimate", "1-2 business days");
            put("price", 1800);
        }});

        List<Map<String, Object>> countries = new ArrayList<>();
        countries.add(new LinkedHashMap<>() {{
            put("code", "US");
            put("name", "United States");
        }});

        List<Map<String, Object>> paymentModes = new ArrayList<>();
        paymentModes.add(new LinkedHashMap<>() {{
            put("id", "approved");
            put("name", "Approve payment");
        }});
        paymentModes.add(new LinkedHashMap<>() {{
            put("id", "declined");
            put("name", "Decline payment");
        }});

        List<Map<String, Object>> categories = page.getContent().stream()
                .collect(Collectors.groupingBy(ProductResponse::getCategory, Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> category = new HashMap<>();
                    category.put("name", entry.getKey());
                    category.put("count", entry.getValue().intValue());
                    return category;
                })
                .sorted((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")))
                .toList();

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("currency", "USD");
        payload.put("taxRate", 0.08);
        payload.put("freeShippingThreshold", 15000);
        payload.put("shippingMethods", shippingMethods);
        payload.put("countries", countries);
        payload.put("paymentModes", paymentModes);
        payload.put("categories", categories);
        payload.put("productCount", page.getTotalElements());
        payload.put("availableCount", page.getContent().stream().filter(p -> p.getStock() > 0).count());
        return payload;
    }

    @Transactional
    public boolean reserveStock(StockReservationRequest request) {
        for (StockReservationRequest.ItemReservation item : request.getItems()) {
            Optional<Product> prodOpt = productRepository.findById(item.getProductId());
            if (prodOpt.isEmpty() || prodOpt.get().getStockQuantity() < item.getQuantity()) {
                return false;
            }
        }
        for (StockReservationRequest.ItemReservation item : request.getItems()) {
            Product p = productRepository.findById(item.getProductId()).get();
            p.setStockQuantity(p.getStockQuantity() - item.getQuantity());
            productRepository.save(p);
        }
        return true;
    }

    @Transactional
    public void releaseStock(StockReservationRequest request) {
        for (StockReservationRequest.ItemReservation item : request.getItems()) {
            productRepository.findById(item.getProductId()).ifPresent(p -> {
                p.setStockQuantity(p.getStockQuantity() + item.getQuantity());
                productRepository.save(p);
            });
        }
    }

    public ProductResponse toResponse(Product product) {
        Map<String, String> specs = Map.of();
        if (product.getSpecs() != null && !product.getSpecs().isBlank()) {
            try {
                specs = objectMapper.readValue(product.getSpecs(), new TypeReference<Map<String, String>>() {});
            } catch (Exception ignored) {}
        }
        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSubtitle() != null ? product.getSubtitle() : "",
                product.getBrand() != null ? product.getBrand() : "Bosch",
                product.getCategory(),
                product.getPrice(),
                product.getStockQuantity(),
                product.isFeatured(),
                product.getImageUrl(),
                product.getDescription(),
                specs
        );
    }
}
