package com.example.similarproducts.application.dto;

import java.math.BigDecimal;

public record ProductDetailDto(String id, String name, BigDecimal price, boolean availability) {
}

