package com.vortise.gestion.infrastructure.mail;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration(proxyBeanMethods = false)
public class PasswordMailConfiguration {
    @Bean(name = "passwordMailExecutor")
    public ThreadPoolTaskExecutor passwordMailExecutor() {
        var executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2); executor.setMaxPoolSize(2); executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("password-mail-");
        executor.setWaitForTasksToCompleteOnShutdown(true); executor.setAwaitTerminationSeconds(10);
        return executor;
    }
}
