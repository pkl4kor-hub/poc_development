package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    List<Order> findByBuyerIdOrderByCreatedAtDesc(Long buyerId);
    Optional<Order> findByBuyerIdAndId(Long buyerId, String id);
    List<Order> findAllByOrderByCreatedAtDesc();
    Optional<Order> findByIdempotencyKey(String idempotencyKey);
}
