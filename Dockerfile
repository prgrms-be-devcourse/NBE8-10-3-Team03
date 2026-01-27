# ====================================
# Stage 1: 빌드 스테이지
# ====================================
FROM gradle:8.5-jdk21 AS builder

WORKDIR /app

# Gradle 래퍼와 설정 파일 복사 (캐싱 최적화)
COPY gradlew .
COPY gradle gradle
COPY build.gradle.kts .
COPY settings.gradle.kts .

# 의존성 다운로드 (레이어 캐싱)
RUN ./gradlew dependencies --no-daemon || return 0

# 소스 코드 복사
COPY src src

# 빌드 수행 (테스트 제외)
RUN ./gradlew bootJar -x test --no-daemon

# ====================================
# Stage 2: 실행 스테이지 (경량화)
# ====================================
FROM eclipse-temurin:21-jre-jammy

WORKDIR /app

# 비루트 사용자 생성 (보안 강화)
RUN groupadd -r spring && useradd -r -g spring spring

# 빌드된 JAR 복사
COPY --from=builder /app/build/libs/*.jar app.jar

# uploads 디렉토리 생성 및 권한 설정
RUN mkdir -p /app/uploads && chown -R spring:spring /app

# 사용자 전환
USER spring:spring

# JVM 메모리 설정 (AWS EC2 t3.micro 기준 - 여유있게)
# - Xms: 초기 힙 메모리 512MB
# - Xmx: 최대 힙 메모리 750MB
# - MaxMetaspaceSize: 메타스페이스 최대 180MB
ENV JAVA_OPTS="-Xms512m -Xmx750m -XX:MaxMetaspaceSize=180m \
               -XX:+UseG1GC \
               -XX:MaxGCPauseMillis=200 \
               -Djava.security.egd=file:/dev/./urandom"

# 애플리케이션 포트
EXPOSE 8080

# 헬스체크 (Spring Boot Actuator)
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/actuator/health || exit 1

# 실행
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]

