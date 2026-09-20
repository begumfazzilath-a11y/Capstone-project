package com.fazzimart.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.fazzimart.model.Order;
import com.fazzimart.model.OrderItem;

public record OrderDTO(
        Long id,
        BigDecimal totalAmount,
        LocalDateTime orderDate,
        String status,
        String customerName,
        String phone,
        String address,
        String city,
        String postalCode,
        List<OrderItemDTO> items) {

    public static OrderDTO from(Order order) {
        List<OrderItemDTO> itemDtos = new ArrayList<>();
        for (OrderItem item : order.getItems()) {
            itemDtos.add(new OrderItemDTO(
                    item.getProductId(),
                    item.getProductName(),
                    item.getQuantity(),
                    item.getPrice(),
                    item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))));
        }
        return new OrderDTO(
                order.getId(),
                order.getTotalAmount(),
                order.getOrderDate(),
                order.getStatus(),
                order.getCustomerName(),
                order.getPhone(),
                order.getAddress(),
                order.getCity(),
                order.getPostalCode(),
                itemDtos);
    }
}