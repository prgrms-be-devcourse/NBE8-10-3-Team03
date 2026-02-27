package com.back.global.config

import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {
    @Bean
    @ConditionalOnBean(RedisConnectionFactory::class)
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        val template = RedisTemplate<String, Any>()
        template.connectionFactory = connectionFactory

        val keySerializer = StringRedisSerializer()
        template.keySerializer = keySerializer
        template.hashKeySerializer = keySerializer

        val valueSerializer: RedisSerializer<Any> = RedisSerializer.json()
        template.valueSerializer = valueSerializer
        template.hashValueSerializer = valueSerializer

        return template
    }
}
