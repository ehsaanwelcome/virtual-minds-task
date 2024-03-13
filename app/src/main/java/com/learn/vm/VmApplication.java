package com.learn.vm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;


@SpringBootApplication
@EnableAsync
@EnableCaching
@Configuration
@EnableConfigurationProperties(RedisProperties.class)
public class VmApplication {
	private static final Logger LOG = LoggerFactory.getLogger(VmApplication.class);
	public static void main(String[] args) {
		SpringApplication.run(VmApplication.class, args);
	}

	@Bean
	public ThreadPoolTaskExecutor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(2);
		//executor.setMaxPoolSize(2);
		//executor.setQueueCapacity(1000);
		executor.setThreadNamePrefix("MyAsyncThread-");
		executor.setRejectedExecutionHandler((r, executor1) -> LOG.warn("Task rejected, thread pool is full and queue is also full"));
		executor.initialize();
		return executor;
	}
	@Bean
	public RedisTemplate<?, ?> redisTemplate(RedisConnectionFactory connectionFactory) {
		RedisTemplate<?, ?> template = new RedisTemplate<>();

		template.setConnectionFactory(connectionFactory);

		return template;
	}
}
