package com.papairs.orchestration.config;

import com.papairs.orchestration.filter.AuthenticationGatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class GatewayConfig {

    private final AuthenticationGatewayFilter authenticationFilter;
    private final ServiceProperties serviceProperties;

    public GatewayConfig(
            AuthenticationGatewayFilter authenticationFilter,
            ServiceProperties serviceProperties) {
        this.authenticationFilter = authenticationFilter;
        this.serviceProperties = serviceProperties;
    }

    /**
     * Defines the routes for the API gateway.
     * <p>
     * This bean creates a {@link RouteLocator} that contains all the routing logic
     * Each route is configured with a path predicate, filters and a destination URI
     * Filters include a custom {@link AuthenticationGatewayFilter} for protected routes
     * and a Circuit Breaker for resilience, which forwards to a fallback URI on failure
     * </p>
     * @param builder The {@link RouteLocatorBuilder} provided by Spring Cloud Gateway to
     * construct the routes in a fluent API style
     * @return A {@link RouteLocator} bean containing all defined application routes.
     */
    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                .route("auth-login", r -> r
                        .path("/api/auth/login")
                        .filters(f -> f
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .circuitBreaker(config -> config
                                        .setName(CircuitBreakerNames.AUTH_SERVICE)
                                        .setFallbackUri("forward:/fallback/auth-service"))
                        )
                        .uri(serviceProperties.authService().url()))

                .route("auth-logout", r -> r
                        .path("/api/auth/logout")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .circuitBreaker(config -> config
                                        .setName(CircuitBreakerNames.AUTH_SERVICE)
                                        .setFallbackUri("forward:/fallback/auth-service"))
                        )
                        .uri(serviceProperties.authService().url()))

                .route("auth-change-password", r -> r
                        .path("/api/auth/change-password")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .circuitBreaker(config -> config
                                        .setName(CircuitBreakerNames.AUTH_SERVICE)
                                        .setFallbackUri("forward:/fallback/auth-service"))
                        )
                        .uri(serviceProperties.authService().url()))

                .route("auth-register", r -> r
                        .path("/api/auth/register")
                        .filters(f -> f
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .circuitBreaker(config -> config
                                        .setName(CircuitBreakerNames.AUTH_SERVICE)
                                        .setFallbackUri("forward:/fallback/auth-service"))
                        )
                        .uri(serviceProperties.authService().url()))

                .route("docs-service-authenticated", r -> r
                        .path("/api/docs/**")
                        .filters(f -> f
                                .filter(authenticationFilter)
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                                .circuitBreaker(config -> config
                                        .setName(CircuitBreakerNames.DOCS_SERVICE)
                                        .setFallbackUri("forward:/fallback/docs-service"))
                        )
                        .uri(serviceProperties.docsService().url()))

                .route("docs-websocket", r -> r
                        .path("/ws/doc/**")
                        .filters(f -> f
                                .removeRequestHeader("Origin")
                                .dedupeResponseHeader("Access-Control-Allow-Origin", "RETAIN_FIRST")
                                .dedupeResponseHeader("Access-Control-Allow-Credentials", "RETAIN_FIRST")
                        )
                        .uri(serviceProperties.docsService().url()))

                .build();
    }
}