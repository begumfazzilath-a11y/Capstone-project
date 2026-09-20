package com.fazzimart.dto;

import java.math.BigDecimal;
import java.util.List;

public record CartResponse(List<CartItemDTO> items, BigDecimal total, int itemCount) {
}