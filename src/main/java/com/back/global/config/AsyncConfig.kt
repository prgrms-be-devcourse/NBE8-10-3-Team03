package com.back.global.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.scheduling.annotation.EnableAsync
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionHandler

/**
 * 비동기 처리를 위한 설정 클래스
 * @EnableAsync: 스프링의 @Async 어노테이션을 활성화하여 비동기 메서드 처리를 가능하게 합니다.
 */
@Configuration
@EnableAsync
class AsyncConfig {

    /**
     * 비동기 작업에 사용할 공용 스레드 풀(Executor)입니다.
     * - "taskExecutor": 스프링 기본 비동기 실행기 이름
     * - "chatTaskExecutor": 채팅 도메인에서 명시적으로 주입받아 사용할 이름
     */
    @Bean(name = ["taskExecutor", "chatTaskExecutor"])
    fun taskExecutor(): Executor {
        // 최소 코어 수를 보장해 로컬/저사양 환경에서도 지나치게 작은 풀 생성을 방지합니다.
        val cpuCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(MIN_CPU_COUNT)

        return ThreadPoolTaskExecutor().apply {
            // CPU 코어 수를 기준으로 기본/최대 풀 크기를 계산합니다.
            corePoolSize = cpuCount
            maxPoolSize = cpuCount * MAX_POOL_MULTIPLIER

            // 큐가 꽉 찬 경우에는 RejectedExecutionHandler에서 호출 스레드가 직접 수행합니다.
            queueCapacity = QUEUE_CAPACITY

            // 로그에서 비동기 스레드를 식별하기 쉽도록 접두사를 부여합니다.
            setThreadNamePrefix(THREAD_NAME_PREFIX)

            // 애플리케이션 종료 시 진행 중인 작업을 최대 N초까지 기다립니다.
            setWaitForTasksToCompleteOnShutdown(true)
            setAwaitTerminationSeconds(AWAIT_TERMINATION_SECONDS)

            // 작업 유실을 줄이기 위해 CallerRunsPolicy와 동일하게 동작하도록 설정합니다.
            setRejectedExecutionHandler(RejectedExecutionHandler { runnable, _ -> runnable.run() })
            initialize()
        }
    }

    companion object {
        private const val MIN_CPU_COUNT = 2
        private const val MAX_POOL_MULTIPLIER = 2
        private const val QUEUE_CAPACITY = 500
        private const val AWAIT_TERMINATION_SECONDS = 30
        private const val THREAD_NAME_PREFIX = "ChatAsync-"
    }
}
