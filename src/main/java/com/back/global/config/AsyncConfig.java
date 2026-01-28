package com.back.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync // 비동기 기능을 활성화
public class AsyncConfig {

    // @Async("taskExecutor")와 매칭
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        // 핵심 스레드 수 (항상 유지할 일꾼 수)
        executor.setCorePoolSize(8);

        // 최대 스레드 수 (부하가 몰릴 때 늘어날 수 있는 최대치)
        executor.setCorePoolSize(16);

        // 큐 용량 (일꾼이 모두 일할 때 대기할 수 있는 작업 바구니 크기)
        executor.setQueueCapacity(500);

        // 스레드 이름 접두사 (로그에서 확인용)
        executor.setThreadNamePrefix("ChatAsync-");

        // 서버 종료 신호가 오면 현재 실행 중인 모든 작업이 완료될 때까지 기다렸다가 종료
        executor.setWaitForTasksToCompleteOnShutdown(true);

        executor.initialize();
        return executor;
    }
}