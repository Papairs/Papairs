package com.papairs.orchestration.service;

import com.papairs.orchestration.client.AuthServiceClient;
import com.papairs.orchestration.dto.response.ValidationResponse;
import com.papairs.orchestration.exception.ServiceUnavailableException;
import org.springframework.cloud.gateway.support.TimeoutException;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class TokenValidationService {

    private final AuthServiceClient authServiceClient;

    public TokenValidationService(AuthServiceClient authServiceClient) {
        this.authServiceClient = authServiceClient;
    }

    /**
     * Validates a bearer token and retrieves the associated user ID
     * <p>Calls the authentication service to validate the token. If successful,
     * extracts and returns the user ID. If the token is invalid or expired,
     * propagates the error for the caller to handle
     * @param token the raw bearer token (without "Bearer " prefix)
     * @return a Mono emitting the user ID if valid
     * @throws ServiceUnavailableException if auth service is unavailable or times out
     * @throws WebClientResponseException for invalid/expired tokens (4xx responses)
     */
    public Mono<String> validateTokenAndGetUserId(String token) {
        return authServiceClient.validateToken(token)
                .map(ValidationResponse::userId)
                .onErrorMap(TimeoutException.class, ex ->
                        new ServiceUnavailableException(
                                "auth-service",
                                "Authentication service timed out",
                                ex
                        )
                )
                .onErrorMap(
                        ex -> ex instanceof WebClientResponseException webEx
                                && webEx.getStatusCode().is5xxServerError(),
                        ex -> new ServiceUnavailableException(
                                "auth-service",
                                "Authentication service is currently unavailable",
                                ex
                        )
                );
    }
}