package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    Optional<CartItem> findByCartIdAndProductId(Long cartId, String productId);
    List<CartItem> findByCartId(Long cartId);
    void deleteByCartId(Long cartId);
}
