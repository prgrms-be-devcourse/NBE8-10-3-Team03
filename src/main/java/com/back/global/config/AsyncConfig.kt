package com.back.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.ThreadPoolExecutor

/**
 * 비동기 처리를 위한 설정 클래스
 * @EnableAsync: 스프링의 @Async 어노테이션을 활성화하여 비동기 메서드 처리를 가능하게 합니다.
 */
@Configuration
@EnableAsync
class AsyncConfig {

    /**
     * 비동기 작업에 사용할 스레드 풀(Executor)을 생성하고 관리합니다.
     * Bean 이름을 "chatTaskExecutor"로도 설정하여 채팅 관련 비동기 작업에 명시적으로 지정할 수 있습니다.
     */
    @Bean(name = ["taskExecutor", "chatTaskExecutor"])
    fun taskExecutor(): Executor = ThreadPoolTaskExecutor().apply {
        // 기본 스레드 수: 항상 살려둘 최소 스레드 개수입니다.
        corePoolSize = CORE_POOL_SIZE
        // 최대 스레드 수: 큐가 꽉 찼을 때 확장 가능한 최대 스레드 개수입니다.
        maxPoolSize = MAX_POOL_SIZE
        // 큐 용량: 작업량이 많아질 때 대기시킬 작업의 최대 개수입니다.
        queueCapacity = QUEUE_CAPACITY
        // 스레드 이름 접두사: 로그 확인 시 비동기 스레드임을 식별하기 쉽게 합니다. (예: ChatAsync-1)
        setThreadNamePrefix(THREAD_NAME_PREFIX)
        // 종료 대기 설정: 애플리케이션 종료 시 진행 중인 작업이 끝날 때까지 기다립니다.
        setWaitForTasksToCompleteOnShutdown(true)
        // 최대 종료 대기 시간: 애플리케이션 종료 시 최대 30초까지 작업 완료를 기다려줍니다.
        setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)
        // 거부 정책: 큐도 꽉 차고 스레드도 최대치일 때, 새로운 요청을 버리지 않고 호출한 스레드(Main)에서 직접 처리하게 합니다.
        // 데이터 누락을 방지하는 안전장치입니다.
        setRejectedExecutionHandler(ThreadPoolExecutor.CallerRunsPolicy())
        // 설정한 옵션들로 스레드 풀을 초기화합니다.
        initialize()
    }

    companion object {
        private const val CORE_POOL_SIZE = 8
        private const val MAX_POOL_SIZE = 16
        private const val QUEUE_CAPACITY = 500
        private const val AWAIT_TERMINATION_SECONDS = 30
        private const val THREAD_NAME_PREFIX = "ChatAsync-"
    }
}
