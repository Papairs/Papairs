package com.papairs.orchestration.client;

import com.papairs.orchestration.config.ServiceProperties;
import com.papairs.orchestration.dto.request.RegisterAccountRequest;
import com.papairs.orchestration.dto.response.RegisterResponse;
import com.papairs.orchestration.dto.response.ValidationResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
public class AuthServiceClient {

    private final WebClient webClient;
    private final Duration timeout;

    public AuthServiceClient(
            WebClient.Builder webClientBuilder,
            ServiceProperties serviceProperties
    ) {
        ServiceProperties.ServiceConfig config = serviceProperties.authService();
        this.webClient = webClientBuilder.baseUrl(config.url()).build();
        this.timeout = Duration.ofSeconds(config.timeoutSeconds());
    }

    /**
     * Validates a given authentication token with the Auth Service.
     * <p>
     * The token is sent in the {@code Authorization} header as a Bearer token
     * </p>
     * @param token The JWT token string to validate
     * @return A {@link reactor.core.publisher.Mono} that emits an {@link com.papairs.orchestration.dto.response.UserResponse}
     * containing the validation result upon success, or an error if the request fails or times out
     */
    public Mono<ValidationResponse> validateToken(String token) {
        return webClient.post()
                .uri("/api/auth/validate")
                .header("Authorization", "Bearer " + token)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .timeout(timeout);
    }

    /**
     * Creates a new user by sending a registration request to the Auth Service
     * @param request The {@link RegisterAccountRequest} containing the details for the new user
     * @return A {@link reactor.core.publisher.Mono} that emits an {@link com.papairs.orchestration.dto.response.RegisterResponse}
     * upon successful user creation
     */
    public Mono<RegisterResponse> createUser(RegisterAccountRequest request) {
        return webClient.post()
                .uri("/api/auth/register")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(RegisterResponse.class)
                .timeout(timeout);
    }

    /**
     * Deletes a user associated with the given user ID on the Auth Service
     * @param userId The unique identifier of the user to be deleted
     * @return A {@link reactor.core.publisher.Mono Mono} that completes successfully
     * if the user is deleted, or emits an error otherwise
     */
    public Mono<Void> deleteUser(String userId) {
        return webClient.delete()
                .uri("/api/auth/" + userId)
                .retrieve()
                .bodyToMono(Void.class)
                .timeout(timeout);
    }
}
