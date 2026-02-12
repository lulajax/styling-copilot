package com.company.fashion.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;
import org.springframework.core.task.TaskDecorator;

@Configuration
@EnableAsync
public class AsyncConfig {

  @Bean(name = "matchExecutor")
  public ThreadPoolTaskExecutor matchExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(4);
    executor.setMaxPoolSize(8);
    executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("match-task-");
    executor.setTaskDecorator(new SecurityContextTaskDecorator());
    executor.initialize();
    return executor;
  }

  /**
   * TaskDecorator that propagates SecurityContext to async threads.
   * This ensures @Async methods can access the current authentication.
   */
  public static class SecurityContextTaskDecorator implements TaskDecorator {
    @Override
    public Runnable decorate(Runnable runnable) {
      return new DelegatingSecurityContextRunnable(runnable);
    }
  }
}
