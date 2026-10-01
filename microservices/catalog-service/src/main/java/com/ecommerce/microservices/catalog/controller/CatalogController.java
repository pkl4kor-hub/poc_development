package com.ecommerce.microservices.catalog.controller;

import com.ecommerce.microservices.catalog.service.CatalogService;
import com.ecommerce.microservices.common.dto.ProductResponse;
import com.ecommerce.microservices.common.dto.StockReservationRequest;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalogService;

    public CatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "ok"));
    }

    @GetMapping("/config")
    public ResponseEntity<Map<String, Object>> config() {
        return ResponseEntity.ok(catalogService.getConfig());
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
        Page<ProductResponse> result = catalogService.search(
                effectiveQuery, category, featured, inStock, minPrice, maxPrice, sort, page, size);

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
        return catalogService.getProductById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/catalog/products/reserve")
    public ResponseEntity<Void> reserveStock(@RequestBody StockReservationRequest request) {
        boolean success = catalogService.reserveStock(request);
        return success ? ResponseEntity.ok().build() : ResponseEntity.badRequest().build();
    }

    @PostMapping("/catalog/products/release")
    public ResponseEntity<Void> releaseStock(@RequestBody StockReservationRequest request) {
        catalogService.releaseStock(request);
        return ResponseEntity.ok().build();
    }
}
