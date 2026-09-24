package com.ecommerce.poc.repository;

import com.ecommerce.poc.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findBySellerId(Long sellerId);
    List<OrderItem> findByOrderId(String orderId);
}
