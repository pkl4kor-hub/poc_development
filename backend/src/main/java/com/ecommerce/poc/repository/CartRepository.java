package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByBuyerIdAndStatus(Long buyerId, String status);
}
