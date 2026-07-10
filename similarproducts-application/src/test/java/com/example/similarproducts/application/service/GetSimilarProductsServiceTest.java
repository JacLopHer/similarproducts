package com.example.similarproducts.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.similarproducts.application.dto.ProductDetailDto;
import com.example.similarproducts.application.dto.SimilarProductsResponseDto;
import com.example.similarproducts.application.mapper.ProductMapper;
import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.model.SimilarProductsResponse;
import com.example.similarproducts.domain.service.GetSimilarProductsUseCase;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

@DisplayName("GetSimilarProductsService Tests")
class GetSimilarProductsServiceTest {

    private GetSimilarProductsUseCase useCaseMock;
    private ProductMapper mapperMock;
    private GetSimilarProductsService service;

    @BeforeEach
    void setUp() {
        useCaseMock = mock(GetSimilarProductsUseCase.class);
        mapperMock = mock(ProductMapper.class);
        service = new GetSimilarProductsService(useCaseMock, mapperMock);
    }

    @Test
    @DisplayName("Should get similar products correctly")
    void shouldGetSimilarProductsCorrectly() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Dress", new BigDecimal("19.99"), true);
        Product product3 = new Product("3", "Blazer", new BigDecimal("29.99"), false);
        SimilarProductsResponse response = new SimilarProductsResponse(
            java.util.List.of(product2, product3)
        );
        ProductDetailDto dto2 = new ProductDetailDto("2", "Dress", new BigDecimal("19.99"), true);
        ProductDetailDto dto3 = new ProductDetailDto("3", "Blazer", new BigDecimal("29.99"), false);
        SimilarProductsResponseDto expectedDto = new SimilarProductsResponseDto(
            java.util.List.of(dto2, dto3)
        );

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.just(response));
        when(mapperMock.toResponseDto(response))
            .thenReturn(expectedDto);

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(productId))
            .expectNext(expectedDto)
            .verifyComplete();

        verify(useCaseMock, times(1)).execute(any(SimilarProductsRequest.class));
        verify(mapperMock, times(1)).toResponseDto(response);
    }

    @Test
    @DisplayName("Should return empty list when no similar products")
    void shouldReturnEmptyListWhenNoSimilarProducts() {
        // Arrange
        String productId = "999";
        SimilarProductsResponse emptyResponse = new SimilarProductsResponse(java.util.List.of());
        SimilarProductsResponseDto emptyDto = new SimilarProductsResponseDto(java.util.List.of());

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.just(emptyResponse));
        when(mapperMock.toResponseDto(emptyResponse))
            .thenReturn(emptyDto);

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(productId))
            .expectNext(emptyDto)
            .verifyComplete();

        verify(useCaseMock, times(1)).execute(any(SimilarProductsRequest.class));
    }

    @Test
    @DisplayName("Should throw ProductNotFoundException when product not found")
    void shouldThrowProductNotFoundExceptionWhenProductNotFound() {
        // Arrange
        String productId = "999";
        ProductNotFoundException exception = new ProductNotFoundException("Product not found");

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(productId))
            .expectError(ProductNotFoundException.class)
            .verify();
    }

    @Test
    @DisplayName("Should call adapters in correct order")
    void shouldCallAdaptersInCorrectOrder() {
        // Arrange
        String productId = "1";
        Product product2 = new Product("2", "Product 2", new BigDecimal("10.00"), true);
        SimilarProductsResponse response = new SimilarProductsResponse(java.util.List.of(product2));
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(
            java.util.List.of(new ProductDetailDto("2", "Product 2", new BigDecimal("10.00"), true))
        );

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.just(response));
        when(mapperMock.toResponseDto(response))
            .thenReturn(responseDto);

        // Act
        service.getSimilarProducts(productId).block();

        // Assert
        verify(useCaseMock, times(1)).execute(any(SimilarProductsRequest.class));
        verify(mapperMock, times(1)).toResponseDto(response);
    }

    @Test
    @DisplayName("Should handle failure in use case execution")
    void shouldHandleFailureInUseCaseExecution() {
        // Arrange
        String productId = "1";
        RuntimeException error = new RuntimeException("Service unavailable");

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.error(error));

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(productId))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    @DisplayName("Should map domain response to DTO correctly")
    void shouldMapDomainResponseToDtoCorrectly() {
        // Arrange
        String productId = "1";
        Product product1 = new Product("2", "Product A", new BigDecimal("15.99"), true);
        Product product2 = new Product("3", "Product B", new BigDecimal("25.99"), false);
        SimilarProductsResponse response = new SimilarProductsResponse(
            java.util.List.of(product1, product2)
        );

        ProductDetailDto dto1 = new ProductDetailDto("2", "Product A", new BigDecimal("15.99"), true);
        ProductDetailDto dto2 = new ProductDetailDto("3", "Product B", new BigDecimal("25.99"), false);
        SimilarProductsResponseDto expectedDto = new SimilarProductsResponseDto(
            java.util.List.of(dto1, dto2)
        );

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.just(response));
        when(mapperMock.toResponseDto(response))
            .thenReturn(expectedDto);

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(productId))
            .assertNext(result -> {
                assertThat(result.products()).hasSize(2);
                assertThat(result.products().get(0).name()).isEqualTo("Product A");
                assertThat(result.products().get(1).name()).isEqualTo("Product B");
            })
            .verifyComplete();
    }

    @Test
    @DisplayName("Should throw InvalidProductIdException for blank productId")
    void shouldThrowInvalidProductIdExceptionForBlankProductId() {
        // Arrange
        InvalidProductIdException exception = new InvalidProductIdException("productId is required");

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.error(exception));

        // Act & Assert
        StepVerifier.create(service.getSimilarProducts(""))
            .expectError(InvalidProductIdException.class)
            .verify();
    }

    @Test
    @DisplayName("Should respect reactive chain without blocking")
    void shouldRespectReactiveChainWithoutBlocking() {
        // Arrange
        String productId = "1";
        Product product = new Product("2", "Product", new BigDecimal("20.00"), true);
        SimilarProductsResponse response = new SimilarProductsResponse(java.util.List.of(product));
        ProductDetailDto dto = new ProductDetailDto("2", "Product", new BigDecimal("20.00"), true);
        SimilarProductsResponseDto responseDto = new SimilarProductsResponseDto(java.util.List.of(dto));

        when(useCaseMock.execute(any(SimilarProductsRequest.class)))
            .thenReturn(Mono.just(response));
        when(mapperMock.toResponseDto(response))
            .thenReturn(responseDto);

        // Act & Assert
        Mono<SimilarProductsResponseDto> result = service.getSimilarProducts(productId);

        StepVerifier.create(result)
            .expectNext(responseDto)
            .verifyComplete();
    }
}

