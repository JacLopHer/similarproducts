package com.example.similarproducts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.dto.SimilarProductsResponseDto;
import com.example.similarproducts.application.service.GetSimilarProductsService;
import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * Unit tests for SimilarProductsController.
 * Validates the REST endpoint for retrieving similar products without needing a full Spring context.
 */
@DisplayName("SimilarProductsController Unit Tests")
class SimilarProductsControllerTest {

    private GetSimilarProductsService serviceMock;
    private SimilarProductsController controller;

    @BeforeEach
    void setUp() {
        serviceMock = mock(GetSimilarProductsService.class);
        controller = new SimilarProductsController(serviceMock);
    }

    @Test
    @DisplayName("Should get similar products and return 200 OK")
    void shouldGetSimilarProductsAndReturn200Ok() {
        // Arrange
        String productId = "1";
        ProductDetailDto product2 = new ProductDetailDto("2", "Shirt", new BigDecimal("19.99"), true);
        ProductDetailDto product3 = new ProductDetailDto("3", "Blazer", new BigDecimal("29.99"), false);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(
            List.of(product2, product3)
        );

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).hasSize(2);
                assertThat(response.get(0).id()).isEqualTo("2");
                assertThat(response.get(1).id()).isEqualTo("3");
            })
            .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should return empty list when no similar products found")
    void shouldReturnEmptyListWhenNoSimilarProductsFound() {
        // Arrange
        String productId = "999";
        SimilarProductsResponseDto emptyResponse = new SimilarProductsResponseDto(List.of());

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(emptyResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response).isEmpty();
            })
            .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should propagate ProductNotFoundException from service")
    void shouldPropagateProductNotFoundExceptionFromService() {
        // Arrange
        String productId = "999";
        ProductNotFoundException exception = new ProductNotFoundException("Product with id 999 not found");

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .expectError(ProductNotFoundException.class)
            .verify();

        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should propagate InvalidProductIdException from service")
    void shouldPropagateInvalidProductIdExceptionFromService() {
        // Arrange
        String productId = "invalid@#$";
        InvalidProductIdException exception = new InvalidProductIdException("Invalid product ID format");

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .expectError(InvalidProductIdException.class)
            .verify();

        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should handle generic runtime exceptions from service")
    void shouldHandleGenericRuntimeExceptionsFromService() {
        // Arrange
        String productId = "1";
        RuntimeException exception = new RuntimeException("Service unavailable");

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .expectError(RuntimeException.class)
            .verify();

        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should map response body correctly from service response")
    void shouldMapResponseBodyCorrectlyFromServiceResponse() {
        // Arrange
        String productId = "1";
        ProductDetailDto product1 = new ProductDetailDto("2", "Product A", new BigDecimal("10.50"), true);
        ProductDetailDto product2 = new ProductDetailDto("3", "Product B", new BigDecimal("25.75"), true);
        ProductDetailDto product3 = new ProductDetailDto("4", "Product C", new BigDecimal("50.00"), false);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(
            List.of(product1, product2, product3)
        );

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                List<ProductDetailDto> products = response;
                assertThat(products).hasSize(3);
                assertThat(products.get(0).name()).isEqualTo("Product A");
                assertThat(products.get(1).name()).isEqualTo("Product B");
                assertThat(products.get(2).name()).isEqualTo("Product C");
                assertThat(products.get(0).availability()).isTrue();
                assertThat(products.get(2).availability()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should extract products field from response DTO")
    void shouldExtractProductsFieldFromResponseDto() {
        // Arrange
        String productId = "42";
        ProductDetailDto product = new ProductDetailDto("100", "Test Product", new BigDecimal("99.99"), true);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of(product));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response).containsExactly(product);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple product requests independently")
    void shouldHandleMultipleProductRequestsIndependently() {
        // Arrange
        String productId1 = "1";
        String productId2 = "2";
        ProductDetailDto product1 = new ProductDetailDto("11", "Product 1", new BigDecimal("10.00"), true);
        ProductDetailDto product2 = new ProductDetailDto("21", "Product 2", new BigDecimal("20.00"), true);
        SimilarProductsResponseDto response1 = new SimilarProductsResponseDto(List.of(product1));
        SimilarProductsResponseDto response2 = new SimilarProductsResponseDto(List.of(product2));

        when(serviceMock.getSimilarProducts(productId1))
            .thenReturn(Mono.just(response1));
        when(serviceMock.getSimilarProducts(productId2))
            .thenReturn(Mono.just(response2));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity1 = controller.getSimilarProducts(productId1);
        StepVerifier.create(responseEntity1.getBody())
            .assertNext(response -> assertThat(response.get(0).id()).isEqualTo("11"))
            .verifyComplete();

        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity2 = controller.getSimilarProducts(productId2);
        StepVerifier.create(responseEntity2.getBody())
            .assertNext(response -> assertThat(response.get(0).id()).isEqualTo("21"))
            .verifyComplete();

        verify(serviceMock, times(1)).getSimilarProducts(productId1);
        verify(serviceMock, times(1)).getSimilarProducts(productId2);
    }

    @Test
    @DisplayName("Should respect reactive nature without blocking")
    void shouldRespectReactiveNatureWithoutBlocking() {
        // Arrange
        String productId = "1";
        ProductDetailDto product = new ProductDetailDto("2", "Product", new BigDecimal("15.99"), true);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of(product));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);

        // Assert - Verify it's still a ResponseEntity with Mono (not subscribed)
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();

        StepVerifier.create(responseEntity.getBody())
            .expectNextCount(1)
            .verifyComplete();

        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should call service with correct product ID parameter")
    void shouldCallServiceWithCorrectProductIdParameter() {
        // Arrange
        String productId = "specific-id-123";
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of());

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act
        controller.getSimilarProducts(productId).getBody().block();

        // Assert
        verify(serviceMock, times(1)).getSimilarProducts("specific-id-123");
    }

    @Test
    @DisplayName("Should handle large number of similar products")
    void shouldHandleLargeNumberOfSimilarProducts() {
        // Arrange
        String productId = "1";
        List<ProductDetailDto> products = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            products.add(new ProductDetailDto(String.valueOf(i), "Product " + i,
                new BigDecimal(i * 10), i % 2 == 0));
        }
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(products);

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).hasSize(100);
            })
            .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should preserve product details in response")
    void shouldPreserveProductDetailsInResponse() {
        // Arrange
        String productId = "1";
        ProductDetailDto product = new ProductDetailDto(
            "detail-id",
            "Detailed Product",
            new BigDecimal("123.45"),
            false
        );
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of(product));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                ProductDetailDto responseProduct = response.get(0);
                assertThat(responseProduct.id()).isEqualTo("detail-id");
                assertThat(responseProduct.name()).isEqualTo("Detailed Product");
                assertThat(responseProduct.price()).isEqualTo(new BigDecimal("123.45"));
                assertThat(responseProduct.availability()).isFalse();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should handle timeout from service")
    void shouldHandleTimeoutFromService() {
        // Arrange
        String productId = "1";
        Exception timeoutException = new Exception("Request timeout");

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.error(timeoutException));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .expectError(Exception.class)
            .verify();

        verify(serviceMock, times(1)).getSimilarProducts(productId);
    }

    @Test
    @DisplayName("Should handle null list in response DTO")
    void shouldHandleNullListInResponseDto() {
        // Arrange
        String productId = "1";
        // SimilarProductsResponseDto has a compact constructor that converts null to empty list
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(null);

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).isNotNull();
                assertThat(response).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return correct HTTP status code 200")
    void shouldReturnCorrectHttpStatusCode200() {
        // Arrange
        String productId = "1";
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of(
            new ProductDetailDto("2", "Product", new BigDecimal("10.00"), true)
        ));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .expectNextCount(1)
            .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Should use ResponseEntity wrapper correctly")
    void shouldUseResponseEntityWrapperCorrectly() {
        // Arrange
        String productId = "1";
        ProductDetailDto product = new ProductDetailDto("2", "Product", new BigDecimal("10.00"), true);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(List.of(product));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(responseDto));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity.getBody())
            .assertNext(response -> {
                assertThat(response).isInstanceOf(List.class);
            })
            .verifyComplete();

        assertThat(responseEntity).isInstanceOf(ResponseEntity.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Constructor should store service dependency")
    void constructorShouldStoreServiceDependency() {
        // Arrange & Act
        SimilarProductsController testController = new SimilarProductsController(serviceMock);

        // Assert - Controller is created without throwing exception
        assertThat(testController).isNotNull();
    }

    @Test
    @DisplayName("Should handle sequential calls to service")
    void shouldHandleSequentialCallsToService() {
        // Arrange
        String productId = "1";
        SimilarProductsResponseDto response1 = new SimilarProductsResponseDto(List.of(
            new ProductDetailDto("2", "Product 1", new BigDecimal("10.00"), true)
        ));
        SimilarProductsResponseDto response2 = new SimilarProductsResponseDto(List.of(
            new ProductDetailDto("3", "Product 2", new BigDecimal("20.00"), true)
        ));

        when(serviceMock.getSimilarProducts(productId))
            .thenReturn(Mono.just(response1))
            .thenReturn(Mono.just(response2));

        // Act & Assert - First call
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity1 = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity1.getBody())
            .assertNext(response -> assertThat(response.get(0).name()).isEqualTo("Product 1"))
            .verifyComplete();

        // Second call
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity2 = controller.getSimilarProducts(productId);
        StepVerifier.create(responseEntity2.getBody())
            .assertNext(response -> assertThat(response.get(0).name()).isEqualTo("Product 2"))
            .verifyComplete();

        verify(serviceMock, times(2)).getSimilarProducts(productId);
    }
}

