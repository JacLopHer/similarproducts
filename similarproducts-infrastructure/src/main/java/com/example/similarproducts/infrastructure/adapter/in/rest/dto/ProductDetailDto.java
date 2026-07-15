package com.example.similarproducts.infrastructure.adapter.in.rest.dto;

import java.math.BigDecimal;

public record ProductDetailDto(String id, String name, BigDecimal price, boolean availability) {
}