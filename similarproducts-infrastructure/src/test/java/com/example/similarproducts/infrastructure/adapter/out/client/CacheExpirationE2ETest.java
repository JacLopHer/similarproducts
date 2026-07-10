package com.example.similarproducts.infrastructure.adapter.out.client;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import reactor.test.StepVerifier;

@Testcontainers
@SpringBootTest(classes = CacheExpirationE2ETest.TestConfiguration.class)
@ActiveProfiles("test")
class CacheExpirationE2ETest {

    @Container
    static GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
        .withExposedPorts(6379);

    private MockWebServer mockWebServer;

    @Autowired
    private WebClient testWebClient;

    @Autowired
    private org.springframework.data.redis.core.RedisTemplate<String, java.util.List<String>> redisTemplate;

    @Value("${cache.ttl.seconds}")
    private long cacheTtlSeconds;

    private SimilarIdsAdapter adapter;

    private ApplicationContext applicationContext;

    @BeforeEach
    void setUp(ApplicationContext applicationContext) throws Exception {
        this.applicationContext = applicationContext;

        // Start mock web server
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        // Create adapter with mock server endpoint
        String mockServerUrl = mockWebServer.url("/").toString() + "product/{productId}/similarids";
        this.adapter = new SimilarIdsAdapter(testWebClient, mockServerUrl);
    }

    @AfterEach
    void tearDown() throws Exception {
        mockWebServer.shutdown();
        // Clear cache after each test
        var cacheManager = applicationContext.getBean(org.springframework.cache.CacheManager.class);
        cacheManager.getCacheNames().forEach(name -> cacheManager.getCache(name).clear());
    }

    @Test
    void testCacheExpirationWithManipulatedTTL() throws InterruptedException {
        // Enqueue responses for both initial call and call after expiration
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("product-1\nproduct-2"));

        // Enqueue second response for after cache expiration
        mockWebServer.enqueue(new MockResponse()
            .setResponseCode(200)
            .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .setBody("product-1\nproduct-2\nproduct-3"));

        StepVerifier.create(adapter.getSimilarIds("456"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-1", "product-2"))
            .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);

        // Wait for cache to expire (TTL is 2 seconds, wait 3 seconds to ensure expiration)
        Thread.sleep(3000);

        // Third call - should hit API again because cache expired
        StepVerifier.create(adapter.getSimilarIds("456"))
            .assertNext(ids -> assertThat(ids).containsExactly("product-1", "product-2", "product-3"))
            .verifyComplete();

        // Verify second request was made to API (cache expired)
        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
    }

    /**
     * Test: Cache expira per productId (distintos productIds tienen cachés independientes)
     */
    @Test
    void testCacheIsDistinctPerProductId() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("a\nb"));

        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("c\nd"));

        // First call to productId "111"
        StepVerifier.create(adapter.getSimilarIds("111").timeout(Duration.ofSeconds(15)))
                .assertNext(ids -> {
                    System.out.println("First call to 111: " + ids);
                    assertThat(ids).containsExactly("a", "b");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(1);
        System.out.println("After first call to 111 - Request count: " + mockWebServer.getRequestCount());

        // First call to productId "222"
        StepVerifier.create(adapter.getSimilarIds("222").timeout(Duration.ofSeconds(15)))
                .assertNext(ids -> {
                    System.out.println("First call to 222: " + ids);
                    assertThat(ids).containsExactly("c", "d");
                })
                .verifyComplete();

        assertThat(mockWebServer.getRequestCount()).isEqualTo(2);
        System.out.println("After first call to 222 - Request count: " + mockWebServer.getRequestCount());

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
                .commandTimeout(java.time.Duration.ofSeconds(30))
                .shutdownTimeout(java.time.Duration.ofSeconds(5))
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
                // Cache store "similar-ids" with SHORT TTL for testing
                .withCacheConfiguration(
                    "similar-ids",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(2))  // 2 seconds for fast testing
                        .disableCachingNullValues()
                )
                // Cache store "product-detail" with SHORT TTL for testing
                .withCacheConfiguration(
                    "product-detail",
                    RedisCacheConfiguration.defaultCacheConfig()
                        .entryTtl(Duration.ofSeconds(2))  // 2 seconds for fast testing
                        .disableCachingNullValues()
                )
                .build();
        }

        /**
         * Create a test WebClient that uses mock server
         */
        @Bean
        WebClient testWebClient() {
            return WebClient.builder().build();
        }

        /**
         * Create RedisTemplate for cache operations
         */
        @Bean
        org.springframework.data.redis.core.RedisTemplate<String, java.util.List<String>> redisTemplate(
            RedisConnectionFactory redisConnectionFactory) {
            org.springframework.data.redis.core.RedisTemplate<String, java.util.List<String>> template =
                new org.springframework.data.redis.core.RedisTemplate<>();
            template.setConnectionFactory(redisConnectionFactory);

            // Configure serialization
            org.springframework.data.redis.serializer.StringRedisSerializer stringSerializer =
                new org.springframework.data.redis.serializer.StringRedisSerializer();
            org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer jsonSerializer =
                new org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer();

            template.setKeySerializer(stringSerializer);
            template.setValueSerializer(jsonSerializer);
            template.setHashKeySerializer(stringSerializer);
            template.setHashValueSerializer(jsonSerializer);

            template.afterPropertiesSet();
            return template;
        }
    }
}


