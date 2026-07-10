package com.example.similarproducts.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Configuración de caching con Redis para adapters OUT
 *
 * Propósito: Cachear respuestas de APIs externas con TTL de 10 minutos
 *
 * Cache stores:
 * - "similar-ids": Cachea lista de IDs similares por productId (10 minutos)
 * - "product-detail": Cachea detalles de producto por productId (10 minutos)
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
            // TTL por defecto: 10 minutos (600 segundos)
            .entryTtl(Duration.ofSeconds(600))
            // Usar valores por defecto para serialización (JSON)
            .disableCachingNullValues();

        return RedisCacheManager.builder(redisConnectionFactory)
            .cacheDefaults(defaultCacheConfig)
            // Cache store "similar-ids" con TTL de 10 minutos
            .withCacheConfiguration(
                "similar-ids",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(600))
                    .disableCachingNullValues()
            )
            // Cache store "product-detail" con TTL de 10 minutos
            .withCacheConfiguration(
                "product-detail",
                RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(600))
                    .disableCachingNullValues()
            )
            .build();
    }

    /**
     * Bean de RedisTemplate para operaciones directas con Redis
     * Utilizado por los adapters OUT (SimilarIdsAdapter, ProductDetailAdapter)
     * para cachear manualmente respuestas de APIs externas
     */
    @Bean
    public RedisTemplate<String, List<String>> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, List<String>> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);

        // Configurar serialización
        StringRedisSerializer stringSerializer = new StringRedisSerializer();
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();

        // Keys: String
        template.setKeySerializer(stringSerializer);
        // Values: JSON
        template.setValueSerializer(jsonSerializer);
        // Hash keys: String
        template.setHashKeySerializer(stringSerializer);
        // Hash values: JSON
        template.setHashValueSerializer(jsonSerializer);

        template.afterPropertiesSet();
        return template;
    }
}

