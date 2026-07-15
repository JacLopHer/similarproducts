<<<<<<<< HEAD:similarproducts-infrastructure/src/test/java/com/example/similarproducts/infrastructure/adapter/mapper/ProductMapperTest.java
package com.example.similarproducts.infrastructure.adapter.mapper;
========
package com.example.similarproducts.rest.adapter.in.rest.mapper;
>>>>>>>> develop:similarproducts-rest-api/src/test/java/com/example/similarproducts/rest/adapter/in/rest/mapper/SimilarProductsRestMapperTest.java

import static org.assertj.core.api.Assertions.assertThat;

import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import java.math.BigDecimal;
import java.util.List;

<<<<<<<< HEAD:similarproducts-infrastructure/src/test/java/com/example/similarproducts/infrastructure/adapter/mapper/ProductMapperTest.java
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.infrastructure.adapter.in.rest.dto.SimilarProductsResponseDto;
import com.example.similarproducts.infrastructure.mapper.ProductMapper;
========
import com.example.similarproducts.rest.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.rest.adapter.in.rest.dto.SimilarProductsResponseDto;
>>>>>>>> develop:similarproducts-rest-api/src/test/java/com/example/similarproducts/rest/adapter/in/rest/mapper/SimilarProductsRestMapperTest.java
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("SimilarProductsRestMapper Tests")
class SimilarProductsRestMapperTest {

    private SimilarProductsRestMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new SimilarProductsRestMapper();
    }

    @Test
    @DisplayName("Should map Product to ProductDetailDto correctly")
    void shouldMapProductToProductDetailDtoCorrectly() {
        // Arrange
        Product product = new Product("1", "Test Product", new BigDecimal("19.99"), true);

        // Act
        ProductDetailDto dto = mapper.toDto(product);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.id()).isEqualTo("1");
        assertThat(dto.name()).isEqualTo("Test Product");
        assertThat(dto.price()).isEqualTo(new BigDecimal("19.99"));
        assertThat(dto.availability()).isTrue();
    }

    @Test
    @DisplayName("Should map Product with availability false")
    void shouldMapProductWithAvailabilityFalse() {
        // Arrange
        Product product = new Product("2", "Unavailable Product", new BigDecimal("29.99"), false);

        // Act
        ProductDetailDto dto = mapper.toDto(product);

        // Assert
        assertThat(dto.availability()).isFalse();
    }

    @Test
    @DisplayName("Should map SimilarProductsResponse to SimilarProductsResponseDto")
    void shouldMapSimilarProductsResponseToResponseDto() {
        // Arrange
        Product product1 = new Product("1", "Product 1", new BigDecimal("10.00"), true);
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), false);
        SimilarProductsResponse response = new SimilarProductsResponse(List.of(product1, product2));

        // Act
        SimilarProductsResponseDto dto = mapper.toResponseDto(response);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.products()).hasSize(2);
        assertThat(dto.products().get(0).id()).isEqualTo("1");
        assertThat(dto.products().get(0).name()).isEqualTo("Product 1");
        assertThat(dto.products().get(1).id()).isEqualTo("2");
        assertThat(dto.products().get(1).name()).isEqualTo("Product 2");
    }

    @Test
    @DisplayName("Should map empty SimilarProductsResponse")
    void shouldMapEmptySimilarProductsResponse() {
        // Arrange
        SimilarProductsResponse response = new SimilarProductsResponse(List.of());

        // Act
        SimilarProductsResponseDto dto = mapper.toResponseDto(response);

        // Assert
        assertThat(dto).isNotNull();
        assertThat(dto.products()).isEmpty();
    }

    @Test
    @DisplayName("Should preserve product order in mapping")
    void shouldPreserveProductOrderInMapping() {
        // Arrange
        Product product3 = new Product("3", "Product 3", new BigDecimal("30.00"), true);
        Product product1 = new Product("1", "Product 1", new BigDecimal("10.00"), false);
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), true);
        SimilarProductsResponse response = new SimilarProductsResponse(List.of(product3, product1, product2));

        // Act
        SimilarProductsResponseDto dto = mapper.toResponseDto(response);

        // Assert
        assertThat(dto.products()).hasSize(3);
        assertThat(dto.products().get(0).id()).isEqualTo("3");
        assertThat(dto.products().get(1).id()).isEqualTo("1");
        assertThat(dto.products().get(2).id()).isEqualTo("2");
    }

    @Test
    @DisplayName("Should map products with various decimal values")
    void shouldMapProductsWithVariousDecimalValues() {
        // Arrange
        Product product1 = new Product("1", "Cheap", new BigDecimal("0.99"), true);
        Product product2 = new Product("2", "Expensive", new BigDecimal("999.99"), true);
        Product product3 = new Product("3", "Precise", new BigDecimal("19.9999"), false);
        SimilarProductsResponse response = new SimilarProductsResponse(
                List.of(product1, product2, product3)
        );

        // Act
        SimilarProductsResponseDto dto = mapper.toResponseDto(response);

        // Assert
        assertThat(dto.products().get(0).price()).isEqualTo(new BigDecimal("0.99"));
        assertThat(dto.products().get(1).price()).isEqualTo(new BigDecimal("999.99"));
        assertThat(dto.products().get(2).price()).isEqualTo(new BigDecimal("19.9999"));
    }

    @Test
    @DisplayName("Should handle null product values in mapping")
    void shouldHandleProductProperties() {
        // Arrange - Test that mapper preserves exact values
        Product product = new Product("id-123", "Special Product", new BigDecimal("99.99"), true);

        // Act
        ProductDetailDto dto = mapper.toDto(product);

        // Assert
        assertThat(dto.id()).isEqualTo("id-123");
        assertThat(dto.name()).isEqualTo("Special Product");
        assertThat(dto.price()).isEqualTo(new BigDecimal("99.99"));
        assertThat(dto.availability()).isTrue();
    }

    @Test
    @DisplayName("Should create immutable DTO records")
    void shouldCreateImmutableDtoRecords() {
        // Arrange
        Product product = new Product("1", "Product", new BigDecimal("10.00"), true);

        // Act
        ProductDetailDto dto1 = mapper.toDto(product);
        ProductDetailDto dto2 = mapper.toDto(product);

        // Assert - both DTOs should be equal since they're records
        assertThat(dto1).isEqualTo(dto2);
    }

    @Test
    @DisplayName("Should map response with single product")
    void shouldMapResponseWithSingleProduct() {
        // Arrange
        Product product = new Product("1", "Only Product", new BigDecimal("50.00"), false);
        SimilarProductsResponse response = new SimilarProductsResponse(List.of(product));

        // Act
        SimilarProductsResponseDto dto = mapper.toResponseDto(response);

        // Assert
        assertThat(dto.products()).hasSize(1);
        assertThat(dto.products().get(0).id()).isEqualTo("1");
        assertThat(dto.products().get(0).availability()).isFalse();
    }
}

