package com.example.similarproducts.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.similarproducts.domain.exception.InvalidProductIdException;
import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.domain.model.SimilarProductsRequest;
import com.example.similarproducts.domain.port.ProductDetailPort;
import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class GetSimilarProductsUseCaseTest {

    @Test
    void shouldReturnProductsInOriginalOrderWhileRemovingDuplicateIds() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of("2", " 3 ", "2", "4"));
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort, 2);

        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .assertNext(response -> assertThat(response.products())
                .extracting(Product::id)
                .containsExactly("2", "3", "4"))
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyResponseWhenNoSimilarIdsExist() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of());
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .assertNext(response -> assertThat(response.products()).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldPropagateProductNotFoundWhenDetailLookupFails() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of("2", "3"));
        ProductDetailPort productDetailPort = productId -> "3".equals(productId)
            ? Mono.error(new ProductNotFoundException("Product 3 not found"))
            : Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ProductNotFoundException.class);
                assertThat(error).hasMessage("Product 3 not found");
            })
            .verify();
    }

    @Test
    void shouldLimitParallelDetailRequestsWithConfiguredConcurrency() {
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();

        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of("2", "3", "4", "5", "6"));
        ProductDetailPort productDetailPort = productId -> Mono.defer(() -> {
            int current = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(current, Math::max);
            return Mono.delay(Duration.ofMillis(25))
                .map(ignored -> {
                    inFlight.decrementAndGet();
                    return product(productId);
                });
        });
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort, 2);

        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .assertNext(response -> assertThat(response.products())
                .extracting(Product::id)
                .containsExactly("2", "3", "4", "5", "6"))
            .verifyComplete();

        assertThat(maxInFlight.get()).isLessThanOrEqualTo(2);
    }

    @Test
    void shouldRejectBlankProductId() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of());
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        assertThatThrownBy(() -> useCase.execute(new SimilarProductsRequest("   ")))
            .isInstanceOf(InvalidProductIdException.class)
            .hasMessage("productId is required");
    }

    @Test
    void shouldRejectNonPositiveConcurrency() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of());
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));

        assertThatThrownBy(() -> new GetSimilarProductsUseCase(similarIdsPort, productDetailPort, 0))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("detailConcurrency must be greater than zero");
    }

    @Test
    void shouldRejectNullRequest() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of());
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        assertThatThrownBy(() -> useCase.execute(null))
            .isInstanceOf(InvalidProductIdException.class)
            .hasMessage("productId is required");
    }

    @Test
    void shouldRejectNullProductId() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of());
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        assertThatThrownBy(() -> useCase.execute(new SimilarProductsRequest(null)))
            .isInstanceOf(InvalidProductIdException.class)
            .hasMessage("productId is required");
    }

    @Test
    void shouldFilterOutEmptyIdsAfterTrimming() {
        SimilarIdsPort similarIdsPort = productId -> Mono.just(List.of("2", "   ", "", "3", "  ", "4"));
        ProductDetailPort productDetailPort = productId -> Mono.just(product(productId));
        GetSimilarProductsUseCase useCase = new GetSimilarProductsUseCase(similarIdsPort, productDetailPort);

        StepVerifier.create(useCase.execute(new SimilarProductsRequest("1")))
            .assertNext(response -> assertThat(response.products())
                .extracting(Product::id)
                .containsExactly("2", "3", "4"))
            .verifyComplete();
    }

    private Product product(String productId) {
        return new Product(productId, "Product " + productId, BigDecimal.TEN, true);
    }
}
