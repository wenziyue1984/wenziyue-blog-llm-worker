package com.wenziyue.llmwork;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * @author wenziyue
 */
@EnableAsync
@EnableScheduling
@ComponentScan("com.wenziyue")
@SpringBootApplication(scanBasePackages = {"com.wenziyue"})
public class LlmWorkerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LlmWorkerApplication.class, args);
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(30))
                .build();
    }
}
