package com.ecommerce.poc.service;

import com.ecommerce.poc.dto.ProductResponse;
import com.ecommerce.poc.entity.Product;
import com.ecommerce.poc.exception.ResourceNotFoundException;
import com.ecommerce.poc.repository.ProductRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CatalogService {
    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CatalogService(ProductRepository productRepository, ObjectMapper objectMapper) {
        this.productRepository = productRepository;
        this.objectMapper = objectMapper;
    }

    public Page<ProductResponse> search(String q, String category, Boolean featured, Boolean inStock, Integer minPrice, Integer maxPrice, String sort, int page, int size) {
        List<Product> all = productRepository.findByActiveTrueOrderByNameAsc();
        String search = q == null ? "" : q.trim().toLowerCase();
        String cat = category == null ? "" : category.trim();
        List<ProductResponse> filtered = all.stream()
                .filter(p -> {
                    if (!search.isEmpty()) {
                        String combined = (p.getName() + " " + (p.getSubtitle() != null ? p.getSubtitle() : "") + " " + (p.getBrand() != null ? p.getBrand() : "") + " " + p.getDescription() + " " + p.getSku() + " " + p.getCategory()).toLowerCase();
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

    public Page<ProductResponse> search(String q, String category, Integer minPrice, Integer maxPrice, String sort, int page, int size) {
        return search(q, category, null, null, minPrice, maxPrice, sort, page, size);
    }

    public ProductResponse getById(String id) {
        Product product = productRepository.findByIdAndActiveTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("PRODUCT_NOT_FOUND", "This tool could not be found."));
        return toResponse(product);
    }

    public List<ProductResponse> listFeatured() {
        return productRepository.findByActiveTrueOrderByNameAsc().stream()
                .filter(Product::isActive)
                .filter(Product::isFeatured)
                .map(this::toResponse)
                .toList();
    }

    private ProductResponse toResponse(Product product) {
        Map<String, String> specs = Map.of();
        try {
            if (product.getSpecs() != null && !product.getSpecs().isBlank()) {
                specs = objectMapper.readValue(product.getSpecs(), new TypeReference<>() {});
            }
        } catch (Exception ignored) {
            specs = Map.of();
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
