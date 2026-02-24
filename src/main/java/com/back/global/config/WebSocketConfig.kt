package com.back.global.config

import com.back.global.security.HttpHandshakeInterceptor
import com.back.global.security.StompHandler
import org.springframework.context.annotation.Configuration
import org.springframework.messaging.simp.config.ChannelRegistration
import org.springframework.messaging.simp.config.MessageBrokerRegistry
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker
import org.springframework.web.socket.config.annotation.StompEndpointRegistry
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer

@Configuration
@EnableWebSocketMessageBroker
class WebSocketConfig(
    private val stompHandler: StompHandler,
    private val httpHandshakeInterceptor: HttpHandshakeInterceptor,
) : WebSocketMessageBrokerConfigurer {

    override fun configureMessageBroker(config: MessageBrokerRegistry) {
        config.enableSimpleBroker("/sub")
        config.setApplicationDestinationPrefixes("/pub")
    }

    override fun registerStompEndpoints(registry: StompEndpointRegistry) {
        registry.addEndpoint("/ws")
            .setAllowedOriginPatterns("*")
            .addInterceptors(httpHandshakeInterceptor)
            .withSockJS()
    }

    // WebSocket 연결 시 JWT 검증을 위한 인터셉터 (StompHandler)
    override fun configureClientInboundChannel(registration: ChannelRegistration) {
        registration.interceptors(stompHandler)
    }
}
