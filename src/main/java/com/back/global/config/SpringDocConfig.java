package com.back.global.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "API 서버", version = "beta", description = "API 서버 문서입니다."))
public class SpringDocConfig {
}
