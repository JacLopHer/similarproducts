package com.example.similarproducts.infrastructure.mapper;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.infrastructure.adapter.out.client.ProductDetailAdapter.ProductDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AdapterMapper {

    private static final Logger logger = LoggerFactory.getLogger(AdapterMapper.class);

    public Product toDomain(ProductDetailResponse response) {
        logger.debug("AdapterMapper - Converting ProductDetailResponse to Domain Product for id: {}", response.id());
        Product product = new Product(response.id(), response.name(), response.price(), response.availability());
        logger.debug("AdapterMapper - Product domain object created for id: {}", product.id());
        return product;
    }
}

