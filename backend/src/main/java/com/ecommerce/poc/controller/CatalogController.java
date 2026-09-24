package com.ecommerce.poc.controller;

import com.ecommerce.poc.dto.ProductResponse;
import com.ecommerce.poc.service.CatalogService;
import com.ecommerce.poc.service.SessionService;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {
    private final CatalogService catalogService;
    private final SessionService sessionService;

    public CatalogController(CatalogService catalogService, SessionService sessionService) {
        this.catalogService = catalogService;
        this.sessionService = sessionService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @PostMapping("/sessions")
    public ResponseEntity<Map<String, String>> createSession() {
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("sessionId", sessionService.createSession()));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        var result = catalogService.search("", "", null, null, "featured", 0, 1000);

        List<Map<String, Object>> shippingMethods = new java.util.ArrayList<>();
        shippingMethods.add(new java.util.LinkedHashMap<>() {{
            put("id", "standard");
            put("name", "Standard delivery");
            put("estimate", "3-5 business days");
            put("price", 850);
        }});
        shippingMethods.add(new java.util.LinkedHashMap<>() {{
            put("id", "express");
            put("name", "Express delivery");
            put("estimate", "1-2 business days");
            put("price", 1800);
        }});

        List<Map<String, Object>> countries = new java.util.ArrayList<>();
        countries.add(new java.util.LinkedHashMap<>() {{
            put("code", "US");
            put("name", "United States");
        }});

        List<Map<String, Object>> paymentModes = new java.util.ArrayList<>();
        paymentModes.add(new java.util.LinkedHashMap<>() {{
            put("id", "approved");
            put("name", "Approve payment");
        }});
        paymentModes.add(new java.util.LinkedHashMap<>() {{
            put("id", "declined");
            put("name", "Decline payment");
        }});

        List<Map<String, Object>> categories = result.getContent().stream()
                .collect(java.util.stream.Collectors.groupingBy(ProductResponse::getCategory, java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .map(entry -> {
                    Map<String, Object> category = new HashMap<>();
                    category.put("name", entry.getKey());
                    category.put("count", entry.getValue().intValue());
                    return category;
                })
                .sorted((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")))
                .toList();

        Map<String, Object> payload = new HashMap<>();
        payload.put("currency", "USD");
        payload.put("taxRate", 0.08);
        payload.put("freeShippingThreshold", 15000);
        payload.put("shippingMethods", shippingMethods);
        payload.put("countries", countries);
        payload.put("paymentModes", paymentModes);
        payload.put("categories", categories);
        payload.put("productCount", result.getTotalElements());
        payload.put("availableCount", result.getContent().stream().filter(product -> product.getStock() > 0).count());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/products")
    public ResponseEntity<Map<String, Object>> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean featured,
            @RequestParam(required = false) Boolean inStock,
            @RequestParam(required = false) Integer minPrice,
            @RequestParam(required = false) Integer maxPrice,
            @RequestParam(defaultValue = "featured") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        String effectiveQuery = (search != null && !search.isBlank()) ? search : q;
        if (size <= 0 || size > 100) {
            size = 20;
        }
        Page<ProductResponse> result = catalogService.search(effectiveQuery, category, featured, inStock, minPrice, maxPrice, sort, page, size);
        Map<String, Object> payload = new HashMap<>();
        payload.put("items", result.getContent());
        payload.put("page", result.getNumber());
        payload.put("size", result.getSize());
        payload.put("totalItems", result.getTotalElements());
        payload.put("totalPages", result.getTotalPages());
        payload.put("total", result.getTotalElements());
        return ResponseEntity.ok(payload);
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductResponse> product(@PathVariable String id) {
        return ResponseEntity.ok(catalogService.getById(id));
    }
}
