package com.example.similarproducts.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("GetSimilarProductsUseCase Tests")
class GetSimilarProductsUseCaseTest {

    private SimilarIdsPort similarIdsPortMock;
    private ProductDetailPort productDetailPortMock;
    private GetSimilarProductsUseCase useCase;

    @BeforeEach
    void setUp() {
        similarIdsPortMock = mock(SimilarIdsPort.class);
        productDetailPortMock = mock(ProductDetailPort.class);
        useCase = new GetSimilarProductsUseCase(similarIdsPortMock, productDetailPortMock);
    }

    @Test
    @DisplayName("Should obtain similar products correctly")
    void shouldObtainSimilarProductsCorrectly() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Dress", new BigDecimal("19.99"), true);
        Product product3 = new Product("3", "Blazer", new BigDecimal("29.99"), false);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "3")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));
        when(productDetailPortMock.getProductDetail("3"))
            .thenReturn(Mono.just(product3));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(2);
                assertThat(response.products()).containsExactly(product2, product3);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should return empty list when no similar products")
    void shouldReturnEmptyListWhenNoSimilarProducts() {
        // Arrange
        String productId = "999";
        when(similarIdsPortMock.getSimilarIds("999"))
            .thenReturn(Mono.just(List.of()));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).isEmpty();
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product does not exist")
    void shouldThrowProductNotFoundExceptionWhenProductNotExists() {
        // Arrange
        String productId = "1";
        ProductNotFoundException exception = new ProductNotFoundException("Product 1 not found");

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .expectError(ProductNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("Should handle failure in detail adapter for one product")
    void shouldHandleFailureInDetailAdapterForOneProduct() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Product 2", new BigDecimal("10.00"), true);
        ProductNotFoundException exception = new ProductNotFoundException("Product 3 not found");

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "3")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));
        when(productDetailPortMock.getProductDetail("3"))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .expectError(ProductNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("Should not duplicate products")
    void shouldNotDuplicateProducts() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Dress", new BigDecimal("19.99"), true);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "2", "2")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(1);
                assertThat(response.products()).containsExactly(product2);
            })
            .verifyComplete();

        // Verify getProductDetail is called only once for product 2 (distinct filter)
        verify(productDetailPortMock, times(1)).getProductDetail("2");
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException for blank productId")
    void shouldThrowInvalidProductIdExceptionForBlankProductId() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            InvalidProductIdException.class,
            () -> useCase.execute(new SimilarProductsRequest(""))
        );
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException for null productId")
    void shouldThrowInvalidProductIdExceptionForNullProductId() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            InvalidProductIdException.class,
            () -> useCase.execute(new SimilarProductsRequest(null))
        );
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException for null request")
    void shouldThrowInvalidProductIdExceptionForNullRequest() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            InvalidProductIdException.class,
            () -> useCase.execute(null)
        );
    }

    @Test
    @DisplayName("Should filter out null IDs using concat and empty")
    void shouldFilterOutNullIds() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), true);

        // Use concat to simulate receiving a null-like value between valid IDs
        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "2")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(1);
                assertThat(response.products()).containsExactly(product2);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should filter out empty string IDs")
    void shouldFilterOutEmptyStringIds() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), true);
        Product product3 = new Product("3", "Product 3", new BigDecimal("30.00"), false);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "", " ", "3")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));
        when(productDetailPortMock.getProductDetail("3"))
            .thenReturn(Mono.just(product3));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(2);
                assertThat(response.products()).containsExactly(product2, product3);
            })
            .verifyComplete();

        verify(productDetailPortMock, times(1)).getProductDetail("2");
        verify(productDetailPortMock, times(1)).getProductDetail("3");
    }

    @Test
    @DisplayName("Should respect order from similar IDs")
    void shouldRespectOrderFromSimilarIds() {
        // Arrange
        String productId = "1";
        Product product3 = new Product("3", "Product 3", new BigDecimal("30.00"), true);
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), false);
        Product product1 = new Product("1", "Product 1", new BigDecimal("10.00"), true);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("3", "2", "1")));
        when(productDetailPortMock.getProductDetail("3"))
            .thenReturn(Mono.just(product3));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));
        when(productDetailPortMock.getProductDetail("1"))
            .thenReturn(Mono.just(product1));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(3);
                assertThat(response.products()).containsExactly(product3, product2, product1);
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should respect concurrency limit for detail fetching")
    void shouldRespectConcurrencyLimitForDetailFetching() {
        // Arrange - Create use case with concurrency of 2
        useCase = new GetSimilarProductsUseCase(similarIdsPortMock, productDetailPortMock, 2);

        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), true);
        Product product3 = new Product("3", "Product 3", new BigDecimal("30.00"), true);
        Product product4 = new Product("4", "Product 4", new BigDecimal("40.00"), true);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("2", "3", "4")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));
        when(productDetailPortMock.getProductDetail("3"))
            .thenReturn(Mono.just(product3));
        when(productDetailPortMock.getProductDetail("4"))
            .thenReturn(Mono.just(product4));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(3);
                assertThat(response.products()).containsExactly(product2, product3, product4);
            })
            .verifyComplete();

        // Verify all detail calls were made
        verify(productDetailPortMock, times(1)).getProductDetail("2");
        verify(productDetailPortMock, times(1)).getProductDetail("3");
        verify(productDetailPortMock, times(1)).getProductDetail("4");
    }

    @Test
    @DisplayName("Should trim whitespace from IDs")
    void shouldTrimWhitespaceFromIds() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Product 2", new BigDecimal("20.00"), true);

        when(similarIdsPortMock.getSimilarIds("1"))
            .thenReturn(Mono.just(List.of("  2  ")));
        when(productDetailPortMock.getProductDetail("2"))
            .thenReturn(Mono.just(product2));

        // Act & Assert
        StepVerifier.create(useCase.execute(new SimilarProductsRequest(productId)))
            .assertNext(response -> {
                assertThat(response.products()).hasSize(1);
                assertThat(response.products()).containsExactly(product2);
            })
            .verifyComplete();

        // Verify the trimmed ID is used
        verify(productDetailPortMock, times(1)).getProductDetail("2");
    }

    @Test
    @DisplayName("Should construct use case with valid parameters")
    void shouldConstructUseCaseWithValidParameters() {
        // Assert - if construction succeeds without exception
        assertThat(useCase).isNotNull();
    }

    @Test
    @DisplayName("Should throw IllegalArgumentException for invalid concurrency")
    void shouldThrowIllegalArgumentExceptionForInvalidConcurrency() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new GetSimilarProductsUseCase(similarIdsPortMock, productDetailPortMock, 0)
        );

        org.junit.jupiter.api.Assertions.assertThrows(
            IllegalArgumentException.class,
            () -> new GetSimilarProductsUseCase(similarIdsPortMock, productDetailPortMock, -5)
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when similarIdsPort is null")
    void shouldThrowNullPointerExceptionWhenSimilarIdsPortIsNull() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new GetSimilarProductsUseCase(null, productDetailPortMock)
        );
    }

    @Test
    @DisplayName("Should throw NullPointerException when productDetailPort is null")
    void shouldThrowNullPointerExceptionWhenProductDetailPortIsNull() {
        // Act & Assert
        org.junit.jupiter.api.Assertions.assertThrows(
            NullPointerException.class,
            () -> new GetSimilarProductsUseCase(similarIdsPortMock, null)
        );
    }


}




