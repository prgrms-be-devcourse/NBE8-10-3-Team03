package com.back.global.app

import com.back.standard.util.Ut
import jakarta.annotation.PostConstruct
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.env.Environment
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import tools.jackson.databind.ObjectMapper

@Configuration
class AppConfig(
    private val environment: Environment,
    private val configuredObjectMapper: ObjectMapper,
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @PostConstruct
    fun postConstruct() {
        Ut.json.objectMapper = configuredObjectMapper
        appEnvironment = environment
        appObjectMapper = configuredObjectMapper
    }

    companion object {
        private var appEnvironment: Environment? = null
        private var appObjectMapper: ObjectMapper? = null

        private fun requireEnvironment(): Environment =
            checkNotNull(appEnvironment) { "Environment is not initialized." }

        private fun requireObjectMapper(): ObjectMapper =
            checkNotNull(appObjectMapper) { "ObjectMapper is not initialized." }

        @JvmStatic
        fun isDev(): Boolean = requireEnvironment().matchesProfiles("dev")

        @JvmStatic
        fun isTest(): Boolean = requireEnvironment().matchesProfiles("test")

        @JvmStatic
        fun isProd(): Boolean = requireEnvironment().matchesProfiles("prod")

        @JvmStatic
        fun isNotProd(): Boolean = !isProd()

        @JvmStatic
        fun getObjectMapper(): ObjectMapper = requireObjectMapper()
    }
}
