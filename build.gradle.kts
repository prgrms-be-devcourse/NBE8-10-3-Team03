plugins {
    java
    kotlin("jvm") version "2.2.20"
    kotlin("plugin.spring") version "2.2.20"
    kotlin("plugin.jpa") version "2.2.20"
    kotlin("plugin.lombok") version "2.2.20"
    id("org.springframework.boot") version "4.0.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.jlleitschuh.gradle.ktlint") version "14.0.1"
}

group = "com"
version = "0.0.1-SNAPSHOT"
description = "DevCource_2"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    compileOnly("org.projectlombok:lombok")
    runtimeOnly("com.h2database:h2")
    developmentOnly("org.springframework.boot:spring-boot-h2console")
    runtimeOnly("com.mysql:mysql-connector-j")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-security-test")
    testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // jjwt
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")

    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.0.0")

    // 웹소켓
    implementation("org.springframework.boot:spring-boot-starter-websocket")
    // OAuth
    implementation("org.springframework.boot:spring-boot-starter-oauth2-client")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-cache")

    // Caffeine 로컬 캐시 (고성능 인메모리 캐시)
    implementation("com.github.ben-manes.caffeine:caffeine:3.1.8")

    // prometheus
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("io.micrometer:micrometer-registry-prometheus")

    // bucket4j
    implementation("com.bucket4j:bucket4j-core:8.10.1")

    // OCI Object Storage
    implementation("com.oracle.oci.sdk:oci-java-sdk-objectstorage:3.58.0")
    implementation("com.oracle.oci.sdk:oci-java-sdk-common-httpclient-jersey:3.58.0")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
