package com.ecommerce.microservices.catalog.repository;

import com.ecommerce.microservices.catalog.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, String> {
    List<Product> findByActiveTrueOrderByNameAsc();
    Optional<Product> findByIdAndActiveTrue(String id);
    List<Product> findByCategoryIgnoreCaseAndActiveTrue(String category);
    List<Product> findByNameContainingIgnoreCaseAndActiveTrue(String name);
}
