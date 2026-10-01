package com.ecommerce.microservices.cart.repository;

import com.ecommerce.microservices.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findBySessionIdAndStatus(String sessionId, String status);
    Optional<Cart> findByBuyerIdAndStatus(Long buyerId, String status);
}
