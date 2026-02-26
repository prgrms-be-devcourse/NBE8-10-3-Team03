package com.back.global.security.port

interface JwtPayloadDecoderPort {
    fun decode(accessToken: String): Map<String, Any>?
}
