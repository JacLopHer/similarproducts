package com.example.similarproducts.infrastructure.config;

import java.time.Duration;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

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

    /**
     * Configurar Redis como CacheManager con TTL por defecto de 10 minutos (600 segundos)
     *
     * Beneficios:
     * - Cachea automáticamente respuestas de APIs externas
     * - Reduce latencia en múltiples llamadas del mismo producto
     * - Expira automáticamente sin invalidación manual
     */
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
}

