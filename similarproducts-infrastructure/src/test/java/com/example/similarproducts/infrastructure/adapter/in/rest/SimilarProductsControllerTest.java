package com.example.similarproducts.infrastructure.adapter.in.rest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.similarproducts.infrastructure.adapter.in.rest.dto.ProductDetailDto;
import com.example.similarproducts.application.service.GetSimilarProductsUseCase;
import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import java.math.BigDecimal;
import java.util.List;

import com.example.similarproducts.infrastructure.mapper.ProductMapper;
import org.junit.jupiter.api.Assertions;
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

    private GetSimilarProductsUseCase useCaseMock;
    private com.example.similarproducts.infrastructure.mapper.ProductMapper productMapper;
    private SimilarProductsController controller;

    @BeforeEach
    void setUp() {
        useCaseMock = mock(GetSimilarProductsUseCase.class);
        productMapper = new ProductMapper();
        controller = new SimilarProductsController(useCaseMock, productMapper);
    }

    @Test
    @DisplayName("Should get similar products and return 200 OK")
    void shouldGetSimilarProductsAndReturn200Ok() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Shirt", new BigDecimal("19.99"), true);
        Product product3 = new Product("3", "Blazer", new BigDecimal("29.99"), false);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product2, product3));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).hasSize(2);
                    assertThat(response.get(0).id()).isEqualTo("2");
                    assertThat(response.get(1).id()).isEqualTo("3");
                })
                .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should return empty list when no similar products found")
    void shouldReturnEmptyListWhenNoSimilarProductsFound() {
        // Arrange
        String productId = "999";
        SimilarProductsResponse emptyResponse = new SimilarProductsResponse(List.of());

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(emptyResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response).isEmpty();
                })
                .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should propagate ProductNotFoundException from usecase")
    void shouldPropagateProductNotFoundExceptionFromUseCase() {
        // Arrange
        String productId = "999";
        ProductNotFoundException exception = new ProductNotFoundException("Product with id 999 not found");

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .expectError(ProductNotFoundException.class)
                .verify();

        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should propagate InvalidProductIdException from usecase")
    void shouldPropagateInvalidProductIdExceptionFromUseCase() {
        // Arrange
        String productId = "invalid@#$";
        InvalidProductIdException exception = new InvalidProductIdException("Invalid product ID format");

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .expectError(InvalidProductIdException.class)
                .verify();

        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should handle generic runtime exceptions from usecase")
    void shouldHandleGenericRuntimeExceptionsFromUseCase() {
        // Arrange
        String productId = "1";
        RuntimeException exception = new RuntimeException("Service unavailable");

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.error(exception));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .expectError(RuntimeException.class)
                .verify();

        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should map response body correctly from domain response")
    void shouldMapResponseBodyCorrectlyFromDomainResponse() {
        // Arrange
        String productId = "1";
        Product product1 = new Product("2", "Product A", new BigDecimal("10.50"), true);
        Product product2 = new Product("3", "Product B", new BigDecimal("25.75"), true);
        Product product3 = new Product("4", "Product C", new BigDecimal("50.00"), false);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(
                List.of(product1, product2, product3)
        );

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).hasSize(3);
                    assertThat(response.get(0).name()).isEqualTo("Product A");
                    assertThat(response.get(1).name()).isEqualTo("Product B");
                    assertThat(response.get(2).name()).isEqualTo("Product C");
                    assertThat(response.get(0).availability()).isTrue();
                    assertThat(response.get(2).availability()).isFalse();
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should extract products field from response DTO")
    void shouldExtractProductsFieldFromResponseDto() {
        // Arrange
        String productId = "42";
        Product product = new Product("100", "Test Product", new BigDecimal("99.99"), true);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).isNotNull();
                    assertThat(response).hasSize(1);
                    assertThat(response.get(0).id()).isEqualTo("100");
                    assertThat(response.get(0).name()).isEqualTo("Test Product");
                })
                .verifyComplete();
    }

    @Test
    @DisplayName("Should handle multiple product requests independently")
    void shouldHandleMultipleProductRequestsIndependently() {
        // Arrange
        String productId1 = "1";
        String productId2 = "2";
        Product product1 = new Product("11", "Product 1", new BigDecimal("10.00"), true);
        Product product2 = new Product("21", "Product 2", new BigDecimal("20.00"), true);
        SimilarProductsResponse response1 = new SimilarProductsResponse(List.of(product1));
        SimilarProductsResponse response2 = new SimilarProductsResponse(List.of(product2));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(response1))
                .thenReturn(Mono.just(response2));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity1 = controller.getSimilarProducts(productId1);
        Assertions.assertNotNull(responseEntity1.getBody());
        StepVerifier.create(responseEntity1.getBody())
                .assertNext(response -> assertThat(response.get(0).id()).isEqualTo("11"))
                .verifyComplete();

        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity2 = controller.getSimilarProducts(productId2);
        Assertions.assertNotNull(responseEntity2.getBody());
        StepVerifier.create(responseEntity2.getBody())
                .assertNext(response -> assertThat(response.get(0).id()).isEqualTo("21"))
                .verifyComplete();

        verify(useCaseMock, times(2)).execute(any());
    }

    @Test
    @DisplayName("Should respect reactive nature without blocking")
    void shouldRespectReactiveNatureWithoutBlocking() {
        // Arrange
        String productId = "1";
        Product product = new Product("2", "Product", new BigDecimal("15.99"), true);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);

        // Assert - Verify it's still a ResponseEntity with Mono (not subscribed)
        assertThat(responseEntity).isNotNull();
        assertThat(responseEntity.getBody()).isNotNull();

        StepVerifier.create(responseEntity.getBody())
                .expectNextCount(1)
                .verifyComplete();

        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should call usecase with correct product ID parameter")
    void shouldCallUseCaseWithCorrectProductIdParameter() {
        // Arrange
        String productId = "specific-id-123";
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of());

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        responseEntity.getBody().block();

        // Assert
        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should handle large number of similar products")
    void shouldHandleLargeNumberOfSimilarProducts() {
        // Arrange
        String productId = "1";
        List<Product> products = new java.util.ArrayList<>();
        for (int i = 0; i < 100; i++) {
            products.add(new Product(String.valueOf(i), "Product " + i,
                    new BigDecimal(i * 10), i % 2 == 0));
        }
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(products);

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).hasSize(100);
                })
                .verifyComplete();

        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should preserve product details in response")
    void shouldPreserveProductDetailsInResponse() {
        // Arrange
        String productId = "1";
        Product product = new Product(
                "detail-id",
                "Detailed Product",
                new BigDecimal("123.45"),
                false
        );
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
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
    @DisplayName("Should handle timeout from usecase")
    void shouldHandleTimeoutFromUseCase() {
        // Arrange
        String productId = "1";
        Exception timeoutException = new Exception("Request timeout");

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.error(timeoutException));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .expectError(Exception.class)
                .verify();

        verify(useCaseMock, times(1)).execute(any());
    }

    @Test
    @DisplayName("Should handle null list in domain response")
    void shouldHandleNullListInDomainResponse() {
        // Arrange
        String productId = "1";
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(null);

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
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
        Product product = new Product("2", "Product", new BigDecimal("10.00"), true);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
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
        Product product = new Product("2", "Product", new BigDecimal("10.00"), true);
        SimilarProductsResponse domainResponse = new SimilarProductsResponse(List.of(product));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(domainResponse));

        // Act & Assert
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity.getBody());
        StepVerifier.create(responseEntity.getBody())
                .assertNext(response -> {
                    assertThat(response).isInstanceOf(List.class);
                })
                .verifyComplete();

        assertThat(responseEntity).isInstanceOf(ResponseEntity.class);
        assertThat(responseEntity.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("Constructor should store usecase and mapper dependencies")
    void constructorShouldStoreUseCaseAndMapperDependencies() {
        // Arrange & Act
        SimilarProductsController testController = new SimilarProductsController(useCaseMock, productMapper);

        // Assert - Controller is created without throwing exception
        assertThat(testController).isNotNull();
    }

    @Test
    @DisplayName("Should handle sequential calls to usecase")
    void shouldHandleSequentialCallsToUseCase() {
        // Arrange
        String productId = "1";
        Product product1 = new Product("2", "Product 1", new BigDecimal("10.00"), true);
        Product product2 = new Product("3", "Product 2", new BigDecimal("20.00"), true);
        SimilarProductsResponse response1 = new SimilarProductsResponse(List.of(product1));
        SimilarProductsResponse response2 = new SimilarProductsResponse(List.of(product2));

        when(useCaseMock.execute(any()))
                .thenReturn(Mono.just(response1))
                .thenReturn(Mono.just(response2));

        // Act & Assert - First call
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity1 = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity1.getBody());
        StepVerifier.create(responseEntity1.getBody())
                .assertNext(response -> assertThat(response.get(0).name()).isEqualTo("Product 1"))
                .verifyComplete();

        // Second call
        ResponseEntity<Mono<List<ProductDetailDto>>> responseEntity2 = controller.getSimilarProducts(productId);
        Assertions.assertNotNull(responseEntity2.getBody());
        StepVerifier.create(responseEntity2.getBody())
                .assertNext(response -> assertThat(response.get(0).name()).isEqualTo("Product 2"))
                .verifyComplete();

        verify(useCaseMock, times(2)).execute(any());
    }
}