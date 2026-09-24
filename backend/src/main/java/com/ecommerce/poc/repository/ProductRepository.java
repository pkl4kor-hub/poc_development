package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, String> {
    Optional<Product> findByIdAndActiveTrue(String id);
    List<Product> findByActiveTrueOrderByNameAsc();

    @Query("SELECT p FROM Product p WHERE p.active = true AND " +
           "(:category IS NULL OR :category = '' OR p.category = :category) AND " +
           "(:minPrice IS NULL OR p.price >= :minPrice) AND " +
           "(:maxPrice IS NULL OR p.price <= :maxPrice) AND " +
           "(:q IS NULL OR :q = '' OR LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.description) LIKE LOWER(CONCAT('%', :q, '%')) OR LOWER(p.category) LIKE LOWER(CONCAT('%', :q, '%'))) " +
           "ORDER BY p.name ASC")
    Page<Product> searchActiveProducts(@Param("q") String q,
                                      @Param("category") String category,
                                      @Param("minPrice") Integer minPrice,
                                      @Param("maxPrice") Integer maxPrice,
                                      Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.name ASC")
    Page<Product> findActiveProducts(Pageable pageable);
}
