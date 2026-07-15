package com.example.similarproducts.rest.adapter.in.rest.dto;

import java.math.BigDecimal;

public record ProductDetailDto(String id, String name, BigDecimal price, boolean availability) {
}