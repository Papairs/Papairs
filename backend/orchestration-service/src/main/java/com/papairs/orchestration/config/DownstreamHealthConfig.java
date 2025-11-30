package com.papairs.orchestration.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Map;

@Configuration
public class DownstreamHealthConfig {

    private final WebClient webClient;

    public DownstreamHealthConfig(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Bean("authService")
    public ReactiveHealthIndicator authServiceHealth() {
        return () -> checkService("http://auth-service:8081/actuator/health");
    }

    @Bean("docsService")
    public ReactiveHealthIndicator docsServiceHealth() {
        return () -> checkService("http://docs-service:8082/actuator/health");
    }

    private Mono<Health> checkService(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(Map.class)
                .map(response -> {
                    String status = (String) response.getOrDefault("status", "UNKNOWN");
                    if ("UP".equalsIgnoreCase(status)) {
                        return Health.up().withDetail("upstream-details", response).build();
                    } else {
                        return Health.down().withDetail("upstream-details", response).build();
                    }
                })

                .onErrorResume(ex -> Mono.just(Health.down()
                        .withException(ex)
                        .withDetail("error", "Service unreachable: " + ex.getMessage())
                        .build()));
    }
}