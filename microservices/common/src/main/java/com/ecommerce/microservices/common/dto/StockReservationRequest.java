package com.ecommerce.microservices.common.dto;

import java.math.BigDecimal;
import java.util.List;

public class StockReservationRequest {
    private String orderId;
    private List<ItemReservation> items;

    public StockReservationRequest() {}

    public StockReservationRequest(String orderId, List<ItemReservation> items) {
        this.orderId = orderId;
        this.items = items;
    }

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }

    public List<ItemReservation> getItems() { return items; }
    public void setItems(List<ItemReservation> items) { this.items = items; }

    public static class ItemReservation {
        private String productId;
        private Integer quantity;

        public ItemReservation() {}

        public ItemReservation(String productId, Integer quantity) {
            this.productId = productId;
            this.quantity = quantity;
        }

        public String getProductId() { return productId; }
        public void setProductId(String productId) { this.productId = productId; }

        public Integer getQuantity() { return quantity; }
        public void setQuantity(Integer quantity) { this.quantity = quantity; }
    }
}
