package com.fazzimart.dto;

import java.math.BigDecimal;

public record CartItemDTO(
        Long productId,
        String name,
        String description,
        String category,
        BigDecimal price,
        String imageUrl,
        Integer quantity,
        BigDecimal subtotal,
        Integer stock) {
}