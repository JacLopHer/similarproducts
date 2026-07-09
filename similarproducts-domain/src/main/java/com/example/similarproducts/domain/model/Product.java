package com.example.similarproducts.domain.model;

import java.math.BigDecimal;

public record Product(String id, String name, BigDecimal price, boolean availability) {
}

