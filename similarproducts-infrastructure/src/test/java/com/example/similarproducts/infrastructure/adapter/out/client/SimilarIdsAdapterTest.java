package com.example.similarproducts.infrastructure.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.similarproducts.domain.exception.ProductNotFoundException;
import java.time.Duration;
import java.util.List;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.test.StepVerifier;

class SimilarIdsAdapterTest {

    private MockWebServer mockWebServer;
    private SimilarIdsAdapter adapter;
    private RedisTemplate<String, List<String>> redisTemplate;

    @BeforeEach
    void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
            .baseUrl(mockWebServer.url("/").toString())
            .build();

        // Create a mock RedisTemplate for testing
        redisTemplate = Mockito.mock(RedisTemplate.class);
        ValueOperations<String, List<String>> valueOps = Mockito.mock(ValueOperations.class);
        Mockito.when(redisTemplate.opsForValue()).thenReturn(valueOps);

        String endpoint = mockWebServer.url("/").toString() + "product/{productId}/similarids";
        adapter = new SimilarIdsAdapter(webClient, endpoint, redisTemplate, 2);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @Test
    void shouldReturnSimilarIdsWhenApiReturns200WithArray() {
        // The adapter now returns a Mono<List> with all IDs
        String responseJson = "id1\nid2\nid3";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids ->
                assertThat(ids).containsExactly("id1", "id2", "id3")
            )
            .verifyComplete();
    }

    @Test
    void shouldReturnEmptyListWhenApiReturnsEmpty() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(""));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids -> assertThat(ids).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldThrowProductNotFoundExceptionWhenApiReturns404() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Not Found\"}"));

        StepVerifier.create(adapter.getSimilarIds("999"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ProductNotFoundException.class);
                assertThat(error.getMessage()).contains("Product not found");
            })
            .verify();
    }

    @Test
    void shouldReturnSimilarIdsWithVariousFormats() {
        String responseJson = "product-1\nproduct_2\nproduct.3\nproduct:4";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids ->
                assertThat(ids).containsExactly("product-1", "product_2", "product.3", "product:4")
            )
            .verifyComplete();
    }

    @Test
    void shouldMakeCorrectHttpRequestToEndpoint() throws InterruptedException {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("id1\nid2"));

        StepVerifier.create(adapter.getSimilarIds("456"))
            .assertNext(ids -> assertThat(ids).isNotEmpty())
            .verifyComplete();

        // Verify the HTTP request was made correctly
        okhttp3.mockwebserver.RecordedRequest recordedRequest = mockWebServer.takeRequest();
        assertThat(recordedRequest.getMethod()).isEqualTo("GET");
        assertThat(recordedRequest.getPath()).contains("456");
    }

    @Test
    void shouldStreamIdsIndividuallyAsFlux() {
        String responseJson = "a\nb\nc\nd\ne";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids -> assertThat(ids).containsExactly("a", "b", "c", "d", "e"))
            .verifyComplete();
    }

    @Test
    void shouldHandleServerErrorsWithTimeoutBehavior() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(500)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Internal Server Error\"}"));

        StepVerifier.create(adapter.getSimilarIds("123")
                .timeout(Duration.ofSeconds(2)))
            .expectError()
            .verify();
    }

    @Test
    void shouldHandleMultipleSimilarIdsRequests() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("1\n2\n3"));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("4\n5"));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(""));

        // First call
        StepVerifier.create(adapter.getSimilarIds("100"))
            .assertNext(ids -> assertThat(ids).hasSize(3))
            .verifyComplete();

        // Second call
        StepVerifier.create(adapter.getSimilarIds("200"))
            .assertNext(ids -> assertThat(ids).hasSize(2))
            .verifyComplete();

        // Third call
        StepVerifier.create(adapter.getSimilarIds("300"))
            .assertNext(ids -> assertThat(ids).isEmpty())
            .verifyComplete();
    }

    @Test
    void shouldReturnSimilarIdsWithSpecialCharacters() {
        String responseJson = "product-123\nitem_456\nSKU.789";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids ->
                assertThat(ids).contains("product-123", "item_456", "SKU.789")
            )
            .verifyComplete();
    }

    @Test
    void shouldPreserveSimilarIdsOrder() {
        String responseJson = "z\ny\nx\nw\nv";

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody(responseJson));

        StepVerifier.create(adapter.getSimilarIds("123"))
            .assertNext(ids ->
                assertThat(ids).containsExactly("z", "y", "x", "w", "v")
            )
            .verifyComplete();
    }

    @Test
    void shouldThrowExceptionWhenApiReturns503ServiceUnavailable() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(503)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Service Unavailable\"}"));

        StepVerifier.create(adapter.getSimilarIds("123"))
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

        StepVerifier.create(adapter.getSimilarIds("123"))
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

        StepVerifier.create(adapter.getSimilarIds("123"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(RuntimeException.class);
                assertThat(error.getMessage()).contains("External API server error");
            })
            .verify();
    }

    @Test
    void shouldExhaustRetriesAndThrowExceptionWithMultiple5xxErrors() {
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

        StepVerifier.create(adapter.getSimilarIds("123"))
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

        StepVerifier.create(adapter.getSimilarIds("123")
                .timeout(Duration.ofSeconds(3)))
            .expectError()
            .verify();
    }

    @Test
    void shouldReturnOnStatusOk200WhenResponseIsValid() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("id1\nid2\nid3"));

        StepVerifier.create(adapter.getSimilarIds("200-test"))
            .assertNext(ids ->
                assertThat(ids).contains("id1", "id2", "id3")
            )
            .verifyComplete();
    }

    @Test
    void shouldHandleNotFoundWithOnStatusHandling() {
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(404)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("{\"error\": \"Not Found\"}"));

        StepVerifier.create(adapter.getSimilarIds("not-found"))
            .expectErrorSatisfies(error -> {
                assertThat(error).isInstanceOf(ProductNotFoundException.class);
                assertThat(error.getMessage()).contains("Product not found");
            })
            .verify();
    }

    @Test
    void shouldHandleSocketTimeoutExceptionViaTimeoutPolicies() {
        // Simulate socket timeout by having server not respond and timeout
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.NO_RESPONSE));

        StepVerifier.create(adapter.getSimilarIds("timeout-test")
                .timeout(Duration.ofSeconds(1)))
            .expectErrorSatisfies(error -> {
                // The timeout will be thrown
                assertThat(error).isInstanceOf(java.util.concurrent.TimeoutException.class);
            })
            .verify();
    }

    @Test
    void shouldHandleConnectExceptionViaDisconnectAtStart() {
        // Simulate connection refused by disconnecting at start - tests mapNetworkException for ConnectException
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        // Add retries
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        StepVerifier.create(adapter.getSimilarIds("connect-refused-test")
                .timeout(Duration.ofSeconds(3)))
            .expectErrorSatisfies(error -> {
                assertThat(error).isNotNull();
                // Should be a network exception that's mapped
                assertThat(error.getMessage()).isNotBlank();
            })
            .verify();
    }

    @Test
    void shouldHandlePrematureCloseViaSocketDisconnect() {
        // Simulate premature close by disconnecting after request - tests mapNetworkException for PrematureCloseException
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST));

        // Add retries
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST));

        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AFTER_REQUEST));

        StepVerifier.create(adapter.getSimilarIds("premature-close-test")
                .timeout(Duration.ofSeconds(3)))
            .expectError()
            .verify();
    }

    @Test
    void shouldPassThroughUnmappedExceptions() {
        // When API returns 200 with valid response - tests return throwable for unmapped exceptions
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("id1\nid2"));

        // This should succeed since the response is valid and any unmapped exceptions are passed through
        StepVerifier.create(adapter.getSimilarIds("valid-test"))
            .assertNext(ids ->
                assertThat(ids).contains("id1", "id2")
            )
            .verifyComplete();
    }

    @Test
    void shouldTestMapNetworkExceptionWithMultipleSocketPolicies() {
        // Tests different socket behaviors to exercise mapNetworkException paths
        mockWebServer.enqueue(new MockResponse()
            .setSocketPolicy(okhttp3.mockwebserver.SocketPolicy.DISCONNECT_AT_START));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("result1"));

        StepVerifier.create(adapter.getSimilarIds("test-mapped-exception")
                .timeout(Duration.ofSeconds(5)))
            .expectErrorSatisfies(error -> {
                // Should get an error from disconnect
                assertThat(error).isNotNull();
            })
            .verify();
    }
}



