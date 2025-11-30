package com.papairs.orchestration.service;

import com.papairs.orchestration.client.AuthServiceClient;
import com.papairs.orchestration.client.DocsServiceClient;
import com.papairs.orchestration.config.CircuitBreakerNames;
import com.papairs.orchestration.dto.request.CreateDocsUserRequest;
import com.papairs.orchestration.dto.request.RegisterAccountRequest;
import com.papairs.orchestration.dto.response.RegisterResponse;
import com.papairs.orchestration.dto.response.UserResponse;
import com.papairs.orchestration.exception.ServiceUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class UserOrchestrationService {

    private final AuthServiceClient authServiceClient;
    private final DocsServiceClient docsServiceClient;
    private final CompensationService compensationService;
    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public UserOrchestrationService(
            AuthServiceClient authServiceClient,
            DocsServiceClient docsServiceClient,
            CompensationService compensationService,
            CircuitBreakerRegistry circuitBreakerRegistry,
            RetryRegistry retryRegistry) {
        this.authServiceClient = authServiceClient;
        this.docsServiceClient = docsServiceClient;
        this.compensationService = compensationService;
        this.circuitBreakerRegistry = circuitBreakerRegistry;
        this.retryRegistry = retryRegistry;
    }

    /**
     * The main entry point for the user creation saga
     * <p>
     * It orchestrates a sequence of two main steps:
     * <ol>
     *     <li>Create the user in the Authentication Service</li>
     *     <li>Create the user's corresponding entry in the Documents Service</li>
     * </ol>
     * If the second step fails, it triggers a compensation action to undo the first step
     * @param request The user registration data
     * @return A {@link Mono} that emits a {@link UserResponse} on successful completion of all steps,
     * or signals an error if the process fails
     */
    public Mono<UserResponse> createUser(RegisterAccountRequest request) {
        return createUserInAuthService(request)
                .flatMap(this::createUserInDocsService);
    }

    /**
     * The first step of the saga: creates an account in the Authentication Service
     * <p>
     * This remote call is protected by a retry mechanism and a circuit breaker
     * 4xx client errors from the auth service are considered terminal and will not be retried
     * Other errors (like 5xx or connection issues) are wrapped in a {@link ServiceUnavailableException}
     * to signal that a retry is appropriate
     * </p>
     * @param request The user registration data
     * @return A {@link Mono} that emits the created {@link UserResponse} on success
     */
    private Mono<UserResponse> createUserInAuthService(RegisterAccountRequest request) {
        return authServiceClient.createUser(request)
                .map(RegisterResponse::user)
                .transformDeferred(RetryOperator.of(retryRegistry.retry(CircuitBreakerNames.AUTH_SERVICE)))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(CircuitBreakerNames.AUTH_SERVICE)))
                .onErrorMap(this::wrapRetryableErrors);
    }

    /**
     * The second step of the saga: creates a user entry in the Documents Service.
     * <p>
     * This call is also protected by a retry mechanism and a circuit breaker.
     * If this step fails after retries, it triggers the compensation logic by calling
     * {@link #handleDocsServiceFailure(String, Throwable)} to ensure data consistency.
     * </p>
     *
     * @param user The user response from the successful creation in the Authentication Service.
     * @return A {@link Mono} that emits the final {@link UserResponse} on success, or triggers
     *         compensation and emits an error on failure.
     */
    private Mono<UserResponse> createUserInDocsService(UserResponse user) {
        CreateDocsUserRequest docsRequest = new CreateDocsUserRequest(user.id());

        return docsServiceClient.createUser(docsRequest)
                .thenReturn(user)
                .transformDeferred(RetryOperator.of(retryRegistry.retry(CircuitBreakerNames.DOCS_SERVICE)))
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(CircuitBreakerNames.DOCS_SERVICE)))
                .onErrorResume(error ->
                        isCircuitBreakerException(error)
                                ? Mono.error(error)
                                : handleDocsServiceFailure(user.id(), error)
                );
    }

    /**
     * Handles failures in the second step of the saga by triggering compensation
     * <p>
     * This method calls the {@link CompensationService} to roll back the user creation
     * in the Authentication Service. After the compensation is complete, it returns a
     * {@link Mono#error(Throwable)} with a specific {@link ServiceUnavailableException}
     * to notify the client of the failure
     * </p>
     * @param userId The ID of the user that needs to be deleted from the auth service
     * @param error  The original error from the docs service call
     * @return A {@link Mono#error(Throwable)} that signals the failure of the overall saga
     */
    private Mono<UserResponse> handleDocsServiceFailure(String userId, Throwable error) {
        return compensationService.compensateUserCreation(userId)
                .then(Mono.error(new ServiceUnavailableException(
                        "docs-service",
                        "Documents service is currently unavailable. Please try again later",
                        error
                )));
    }

    /**
     * A helper method to transform errors and distinguish between retryable and non-retryable failures
     * <p>
     * - 4xx client errors are considered final and are not wrapped, preventing retries
     * - Circuit breaker exceptions are also passed through directly
     * - All other exceptions (e.g., 5xx server errors, timeouts) are wrapped in a
     *   {@link ServiceUnavailableException} to indicate they might be transient
     * </p>
     * @param error The original throwable from a reactive pipeline
     * @return The original error if it's non-retryable, or a wrapped {@link ServiceUnavailableException}
     */
    private Throwable wrapRetryableErrors(Throwable error) {
        if (error instanceof WebClientResponseException webEx
                && webEx.getStatusCode().is4xxClientError()) {
            return error;
        }

        if (error instanceof CallNotPermittedException) {
            return error;
        }

        return new ServiceUnavailableException(
                "auth-service",
                "Authentication service is currently unavailable. Please try again later",
                error
        );
    }

    /**
     * Checks if a given error is a {@link CallNotPermittedException}
     * @param error The throwable to check
     * @return {@code true} if the error indicates the circuit breaker is open, otherwise {@code false}
     */
    private boolean isCircuitBreakerException(Throwable error) {
        return error instanceof CallNotPermittedException;
    }
}
