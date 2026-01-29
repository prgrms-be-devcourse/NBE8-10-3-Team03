package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 설정 (WebSocket/채팅용)
 *
 * 주의: 캐싱은 Caffeine 사용 (CacheConfig 참조)
 * - RedisTemplate: WebSocket, 채팅 메시지 등에 사용
 * - CacheManager: CacheConfig의 Caffeine 사용
 */
@Configuration
public class RedisConfig {

    /**
     * RedisTemplate 설정 (WebSocket/채팅용)
     * - Key: String 직렬화
     * - Value: JSON 직렬화
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key는 String으로 직렬화
        StringRedisSerializer keySerializer = new StringRedisSerializer();
        template.setKeySerializer(keySerializer);
        template.setHashKeySerializer(keySerializer);

        // Value는 JSON으로 직렬화 (Spring Boot 4.0 방식 - LocalDateTime 자동 지원)
        RedisSerializer<Object> valueSerializer = RedisSerializer.json();
        template.setValueSerializer(valueSerializer);
        template.setHashValueSerializer(valueSerializer);

        return template;
    }
}

