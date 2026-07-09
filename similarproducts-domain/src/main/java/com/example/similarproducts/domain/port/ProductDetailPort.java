package com.example.similarproducts.domain.port;

import com.example.similarproducts.domain.model.Product;

public interface ProductDetailPort {

    Product getProductDetail(String productId);
}

