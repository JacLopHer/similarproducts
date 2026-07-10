package com.example.similarproducts.infrastructure.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import com.example.similarproducts.domain.model.Product;
import com.example.similarproducts.infrastructure.mapper.AdapterMapper;
import java.math.BigDecimal;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class ProductDetailAdapterTest {

    private MockWebServer mockWebServer;
    private ProductDetailAdapter adapter;
    private AdapterMapper adapterMapper;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        adapterMapper = new AdapterMapper();
        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        String endpoint = mockWebServer.url("/").toString() + "product/{productId}";
        adapter = new ProductDetailAdapter(webClient, adapterMapper, endpoint);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnProductDetailWhenApiReturns200() {
        String responseJson = """
            {
                "id": "123",
                "name": "Laptop",
                "price": 999.99,
                "availability": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getProductDetail("123"))
            .assertNext(product -> {
                assertThat(product.id()).isEqualTo("123");
                assertThat(product.name()).isEqualTo("Laptop");
                assertThat(product.price()).isEqualTo(new BigDecimal("999.99"));
                assertThat(product.availability()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenApiReturns404() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Not Found\"}"));

        StepVerifier.create(adapter.getProductDetail("999"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ProductNotFoundException.class);
                assertThat(error.getMessage()).contains("Product not found");
            })
            .verify();
    }

    @Test
    void shouldReturnProductDetailWithVariousPrices() {
        String[] prices = {"10.50", "0.01", "99999.99"};

        for (String price : prices) {
            String responseJson = String.format("""
                {
                    "id": "product_%s",
                    "name": "Product with price %s",
                    "price": %s,
                    "availability": false
                }
                """, price.replace(".", "_"), price, price);

            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseJson));

            StepVerifier.create(adapter.getProductDetail("product_" + price.replace(".", "_")))
                .assertNext(product -> {
                    assertThat(product.price()).isEqualTo(new BigDecimal(price));
                    assertThat(product.availability()).isFalse();
                })
                .verifyComplete();
        }
    }

    @Test
    void shouldHandleServerErrorsWithTimeoutBehavior() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Internal Server Error\"}"));

        StepVerifier.create(adapter.getProductDetail("123")
                .timeout(Duration.ofSeconds(2)))
            .expectError()
            .verify();
    }

    @Test
    void shouldMakeCorrectHttpRequestToEndpoint() throws InterruptedException {
        String responseJson = """
            {
                "id": "456",
                "name": "Mouse",
                "price": 25.50,
                "availability": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getProductDetail("456"))
            .assertNext(product -> assertThat(product.id()).isEqualTo("456"))
            .verifyComplete();

        // Verify the HTTP request was made correctly
        okhttp3.mockwebserver.RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).contains("456");
    }

    @Test
    void shouldParseJsonResponseCorrectly() {
        String responseJson = """
            {
                "id": "789",
                "name": "Keyboard with special chars: áéíóú",
                "price": 79.99,
                "availability": false
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getProductDetail("789"))
            .assertNext(product -> {
                assertThat(product.name()).isEqualTo("Keyboard with special chars: áéíóú");
                assertThat(product.price()).isEqualTo(new BigDecimal("79.99"));
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleMultipleProductDetailRequests() {
        for (int i = 1; i <= 3; i++) {
            String responseJson = String.format("""
                {
                    "id": "%d",
                    "name": "Product %d",
                    "price": %d.00,
                    "availability": %s
                }
                """, i, i, i * 100, i % 2 == 0);

            mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody(responseJson));
        }

        // Test first product
        StepVerifier.create(adapter.getProductDetail("1"))
            .assertNext(product -> {
                assertThat(product.id()).isEqualTo("1");
                assertThat(product.name()).isEqualTo("Product 1");
            })
            .verifyComplete();

        // Test second product
        StepVerifier.create(adapter.getProductDetail("2"))
            .assertNext(product -> {
                assertThat(product.availability()).isTrue();
            })
            .verifyComplete();

        // Test third product
        StepVerifier.create(adapter.getProductDetail("3"))
            .assertNext(product -> {
                assertThat(product.price()).isEqualTo(new BigDecimal("300.00"));
            })
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenApiReturns503ServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(503)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Service Unavailable\"}"));

        StepVerifier.create(adapter.getProductDetail("123"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(RuntimeException.class);
                assertThat(error.getMessage()).contains("External API server error");
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenApiReturns502BadGateway() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Bad Gateway\"}"));

        StepVerifier.create(adapter.getProductDetail("123"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(RuntimeException.class);
                assertThat(error.getMessage()).contains("External API server error");
            })
            .verify();
    }

    @Test
    void shouldThrowExceptionWhenApiReturns500InternalServerError() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Internal Server Error\"}"));

        StepVerifier.create(adapter.getProductDetail("123"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(RuntimeException.class);
                assertThat(error.getMessage()).contains("External API server error");
            })
            .verify();
    }

    @Test
    void shouldExhaustRetriesAndThrowExceptionWithMultiple5xxErrors() {
        // Enqueue multiple 5xx responses to exceed MAX_RETRIES (2)
        // First request
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Internal Server Error\"}"));

        // Retry 1
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(503)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Service Unavailable\"}"));

        // Retry 2
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(502)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Bad Gateway\"}"));

        StepVerifier.create(adapter.getProductDetail("123"))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void shouldMapIOExceptionInRetryPolicy() {
        // Simulate IOException through socket disconnect
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        // Add another error after retry exhaustion
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Server Error\"}"));

        // Add another error after retry exhaustion
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Server Error\"}"));

        StepVerifier.create(adapter.getProductDetail("123")
                .timeout(Duration.ofSeconds(3)))
            .expectError()
            .verify();
    }

    @Test
    void shouldReturnOnStatusOk200WhenResponseIsValid() {
        String responseJson = """
            {
                "id": "200-test",
                "name": "Success Response",
                "price": 150.00,
                "availability": true
            }
            """;

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getProductDetail("200-test"))
            .assertNext(product -> {
                assertThat(product.id()).isEqualTo("200-test");
                assertThat(product.name()).isEqualTo("Success Response");
                assertThat(product.price()).isEqualTo(new BigDecimal("150.00"));
                assertThat(product.availability()).isTrue();
            })
            .verifyComplete();
    }

    @Test
    void shouldHandleNotFoundWithOnStatusHandling() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Not Found\"}"));

        StepVerifier.create(adapter.getProductDetail("not-found"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ProductNotFoundException.class);
                assertThat(error.getMessage()).contains("Product not found");
            })
            .verify();
    }
}


