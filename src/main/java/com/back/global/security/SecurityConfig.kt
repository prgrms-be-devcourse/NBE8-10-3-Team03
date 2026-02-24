package com.back.global.security

import com.back.global.rsData.RsData
import com.back.standard.util.Ut
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.AuthenticationSuccessHandler
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val customAuthenticationFilter: CustomAuthenticationFilter,
    private val apiRateLimitFilter: ApiRateLimitFilter,
    private val customOAuth2LoginSuccessHandler: AuthenticationSuccessHandler,
    private val customOAuth2AuthorizationRequestResolver: CustomOAuth2AuthorizationRequestResolver
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .authorizeHttpRequests { auth ->
                auth
                    .requestMatchers("/api/*/members/login", "/api/*/members/join", "/api/*/members/logout").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/*/members").permitAll()
                    .requestMatchers(HttpMethod.PATCH, "/api/*/members/me/profile").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/*/auctions/**", "/api/*/posts/**", "/api/*/search").permitAll()
                    .requestMatchers("/api/*/auctions/**").authenticated()
                    .requestMatchers("/api/*/**").authenticated()
                    .requestMatchers("/ws/**").permitAll()
                    .requestMatchers(HttpMethod.POST, "/api/chat/send", "/api/chat/room").authenticated()
                    .requestMatchers("/api/chat/list").authenticated()
                    .requestMatchers("/api/**").permitAll()
                    .anyRequest().permitAll()
            }
            .headers { headers ->
                headers.frameOptions { it.sameOrigin() }
            }
            .csrf { it.disable() }
            .formLogin { it.disable() }
            .logout { it.disable() }
            .httpBasic { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .oauth2Login { oauth2 ->
                oauth2
                    .successHandler(customOAuth2LoginSuccessHandler)
                    .authorizationEndpoint { endpoint ->
                        endpoint.authorizationRequestResolver(customOAuth2AuthorizationRequestResolver)
                    }
            }
            .addFilterBefore(customAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .addFilterAfter(apiRateLimitFilter, CustomAuthenticationFilter::class.java)
            .exceptionHandling { exceptions ->
                exceptions
                    .authenticationEntryPoint { _, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = 401
                        response.writer.write(Ut.json.toString(RsData<Void?>("401-1", "로그인 후 이용해주세요.")))
                    }
                    .accessDeniedHandler { _, response, _ ->
                        response.contentType = "application/json;charset=UTF-8"
                        response.status = 403
                        response.writer.write(Ut.json.toString(RsData<Void?>("403-1", "권한이 없습니다.")))
                    }
            }

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): UrlBasedCorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = listOf("https://cdpn.io", "http://localhost:3000")
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }

        return UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/api/**", configuration)
        }
    }
}