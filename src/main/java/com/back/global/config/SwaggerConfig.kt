package com.back.global.config

import io.swagger.v3.oas.models.Components
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.security.SecurityRequirement
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {
    @Bean
    fun openAPI(): OpenAPI {
        val securityScheme = SecurityScheme()
            .type(SecurityScheme.Type.HTTP)
            .scheme("bearer")
            .bearerFormat("JWT")
            .`in`(SecurityScheme.In.HEADER)
            .name("Authorization")

        val securityRequirement = SecurityRequirement()
            .addList("bearerAuth")

        return OpenAPI()
            .info(apiInfo())
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("개발 서버"),
                    Server().url("https://api.example.com").description("운영 서버"),
                ),
            )
            .components(Components().addSecuritySchemes("bearerAuth", securityScheme))
            .addSecurityItem(securityRequirement)
    }

    private fun apiInfo(): Info =
        Info()
            .title("경매 중고 거래 플랫폼 API")
            .description(
                """
                ## 경매 및 중고 거래 플랫폼 REST API 문서

                ### 주요 기능
                - **경매 관리**: 경매 물품 등록, 조회, 수정, 삭제
                - **입찰 관리**: 입찰하기, 입찰 내역 조회, 즉시구매
                - **낙찰 관리**: 자동 낙찰, 거래 취소
                - **회원 관리**: 회원가입, 로그인, 신용도 관리
                - **중고 거래**: 중고 물품 등록 및 거래
                - **검색**: 통합 검색 기능

                ### 인증 방식
                - **JWT Bearer Token** 사용
                - 로그인 후 발급받은 accessToken을 Header에 포함
                - 형식: `Authorization: Bearer {accessToken}`

                ### 응답 형식
                모든 API는 다음 형식으로 응답합니다:
                ```json
                {
                  "resultCode": "200-1",
                  "msg": "성공 메시지",
                  "data": { ... }
                }
                ```

                ### 에러 코드
                - **400**: 잘못된 요청 (유효성 검증 실패 등)
                - **401**: 인증 실패 (로그인 필요)
                - **403**: 권한 없음
                - **404**: 리소스를 찾을 수 없음
                - **500**: 서버 내부 오류
                """.trimIndent(),
            )
            .version("1.0.0")
            .contact(
                io.swagger.v3.oas.models.info.Contact()
                    .name("DevCource Team")
                    .email("support@example.com"),
            )
}
