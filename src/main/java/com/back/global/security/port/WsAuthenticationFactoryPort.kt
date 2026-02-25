package com.back.global.security.port

import org.springframework.security.core.Authentication

interface WsAuthenticationFactoryPort {
    fun fromPayload(payload: Map<String, Any>): Authentication
}
