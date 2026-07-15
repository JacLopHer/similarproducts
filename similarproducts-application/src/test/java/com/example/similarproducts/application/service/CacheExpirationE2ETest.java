package com.example.similarproducts.application.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.similarproducts.domain.port.SimilarIdsPort;
import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

/**
 * E2E test for cache expiration behavior of SimilarIdsCacheHelper with Redis.
 * Tests that:
 * - Cache is stored and reused
 * - Cache expires after TTL
 * - Different productIds have independent caches
 */
@Testcontainers
@SpringBootTest(classes = CacheExpirationE2ETest.TestConfiguration.class)
@ActiveProfiles("test")
class CacheExpirationE2ETest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    static MockWebServer mockWebServer;

    @Autowired
    private SimilarIdsCacheHelper cacheHelper;

    @Autowired
    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp() throws Exception {
        // Start mock web server
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
        // Clear cache after each test
        var cacheManager = applicationContext.getBean(org.springframework.cache.CacheManager.class);
        cacheManager.getCacheNames().forEach(name -> {
            var cache = cacheManager.getCache(name);
            if (cache != null) {
                cache.clear();
            }
        });
    }

    /**
     * Test that cache expires after TTL (2 seconds for testing)
     * and subsequent calls hit the API again.
     */
    @Test
    void testCacheExpirationAfterTTL() throws InterruptedException {
        // Arrange: First response
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("[\"product-1\",\"product-2\"]"));

        // Second response after cache expiration
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("[\"product-1\",\"product-2\",\"product-3\"]"));

        // Act: First call - hits API
        StepVerifier.create(cacheHelper.getCachedSimilarIds("456"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-1", "product-2"))
            .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        // Wait for cache to expire (TTL is 2 seconds, wait 3 seconds to ensure expiration)
        Thread.sleep(3000);

        // Act: Second call after expiration - should hit API again
        StepVerifier.create(cacheHelper.getCachedSimilarIds("456"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-1", "product-2", "product-3"))
            .verifyComplete();

        // Assert: Verify second request was made to API
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    /**
     * Test that cache is reused within the TTL (2 seconds)
     * and does not call the API again for the same productId.
     */
    @Test
    void testCacheReuseWithinTTL() {
        // Arrange: Only one response, since cache should prevent second call
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("[\"product-a\",\"product-b\"]"));

        // Act: First call - hits API and caches result
        StepVerifier.create(cacheHelper.getCachedSimilarIds("789"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-a", "product-b"))
            .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        // Act: Second call immediately - should use cache, no API call
        StepVerifier.create(cacheHelper.getCachedSimilarIds("789"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-a", "product-b"))
            .verifyComplete();

        // Assert: Still only 1 request because cache was used
        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
    }

    /**
     * Test that different productIds have independent caches
     */
    @Test
    void testCacheIsDistinctPerProductId() {
        // Arrange: Two different responses for two different productIds
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("[\"a\",\"b\"]"));

        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("[\"c\",\"d\"]"));

        // Act: First call with productId "111"
        StepVerifier.create(cacheHelper.getCachedSimilarIds("111").timeout(Duration.ofSeconds(15)))
            .assertNext(ids -> assertThat(ids).containsExactly("a", "b"))
            .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        // Act: First call with productId "222" (different from "111")
        StepVerifier.create(cacheHelper.getCachedSimilarIds("222").timeout(Duration.ofSeconds(15)))
            .assertNext(ids -> assertThat(ids).containsExactly("c", "d"))
            .verifyComplete();

        // Assert: Two requests because they are different productIds with different caches
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);

        // Act: Second call with productId "111" - should use its cache
        StepVerifier.create(cacheHelper.getCachedSimilarIds("111").timeout(Duration.ofSeconds(15)))
            .assertNext(ids -> assertThat(ids).containsExactly("a", "b"))
            .verifyComplete();

        // Assert: Still 2 requests, because "111"'s result was cached
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    /**
     * Test configuration for E2E tests with Redis
     */
    @Configuration
    @EnableCaching
    static class TestConfiguration {

        /**
         * Create Redis connection factory using TestContainer
         */
        @Bean
        RedisConnectionFactory redisConnectionFactory() {
            // Configure Lettuce client with proper timeouts
            var clientConfig = org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration
                .builder()
                .commandTimeout(Duration.ofSeconds(30))
                .shutdownTimeout(Duration.ofSeconds(5))
                .build();

            var standaloneConfig = new org.springframework.data.redis.connection.RedisStandaloneConfiguration();
            standaloneConfig.setHostName(redis.getHost());
            standaloneConfig.setPort(redis.getFirstMappedPort());

            return new LettuceConnectionFactory(standaloneConfig, clientConfig);
        }

        /**
         * Create cache manager with SHORT TTL for testing (2 seconds instead of 600)
         * This allows testing cache expiration quickly without waiting 10 minutes.
         */
        @Bean
        @Primary
        org.springframework.cache.CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
            RedisCacheConfiguration shortTtlCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                // SHORT TTL for testing: 2 seconds (instead of 600 for production)
                .entryTtl(Duration.ofSeconds(2))
                .disableCachingNullValues();

            return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(shortTtlCacheConfig)
                .withCacheConfiguration(
                    "similar-ids",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(2))  // 2 seconds for fast testing
                        .disableCachingNullValues()
                )
                .build();
        }

        /**
         * Create a mock SimilarIdsPort that delegates to WebClient
         */
        @Bean
        SimilarIdsPort mockSimilarIdsPort() {
            return productId -> {
                if (productId == null || productId.isBlank()) {
                    return Mono.just(java.util.List.of());
                }
                // MockWebServer must be initialized before use
                if (mockWebServer == null) {
                    return Mono.error(new IllegalStateException("MockWebServer not initialized"));
                }
                return WebClient.builder().build()
                    .get()
                    .uri("http://localhost:" + mockWebServer.getPort() + "/product/" + productId + "/similarids")
                    .retrieve()
                    .bodyToMono(new org.springframework.core.ParameterizedTypeReference<java.util.List<String>>() {})
                    .onErrorResume(e -> Mono.just(java.util.List.of()));
            };
        }

        /**
         * Create the SimilarIdsCacheHelper that we're testing
         */
        @Bean
        SimilarIdsCacheHelper similarIdsCacheHelper(SimilarIdsPort similarIdsPort) {
            return new SimilarIdsCacheHelper(similarIdsPort);
        }

        /**
         * Create RedisTemplate for cache operations
         */
        @Bean
        RedisTemplate<String, java.util.List<String>> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
            RedisTemplate<String, java.util.List<String>> template = new RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory);

            StringRedisSerializer stringSerializer = new StringRedisSerializer();
            GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(jsonSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(jsonSerializer);

            template.afterPropertiesSet();
            return template;
        }
    }
}


