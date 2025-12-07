package com.papairs.orchestration.e2e.routing;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@DisplayName("Circuit Breaker and Fallback")
public class CircuitBreakerTest extends AbstractE2ETest {

    @Nested
    @DisplayName("Docs Service Fallback")
    class DocsServiceFallback {

        @Test
        @DisplayName("Should return fallback when docs service is down")
        public void returnFallbackWhenDocsServiceDown() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubServiceDown();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.error").isEqualTo("Service Unavailable")
                    .jsonPath("$.message").value(containsString("Documents service"))
                    .jsonPath("$.message").value(containsString("unavailable"));
        }

        @Test
        @DisplayName("Should return fallback when docs service returns 503")
        public void returnFallbackWhenDocsServiceReturns503() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubServiceUnavailable();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("Should return fallback when docs service times out")
        public void returnFallbackWhenDocsServiceTimesOut() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubServiceTimeout(5000);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.message").value(containsString("unavailable"));
        }

        @Test
        @DisplayName("Should include fallback path in fallback response")
        public void includeFallbackPathInResponse() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubServiceDown();

            webTestClient.get()
                    .uri("/api/docs/folders/specific-folder")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.path").isEqualTo("/fallback/docs-service");
        }
    }

    @Nested
    @DisplayName("Auth Service Fallback (Login Route)")
    class AuthServiceFallback {

        @Test
        @DisplayName("Should return fallback when auth service is down during login")
        public void returnFallbackWhenAuthServiceDownDuringLogin() throws Exception {
            authMock.stubLoginConnectionReset();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"test@example.com\", \"password\": \"password123\"}")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.status").isEqualTo(503)
                    .jsonPath("$.message").value(containsString("Authentication service"))
                    .jsonPath("$.message").value(containsString("unavailable"));
        }

        @Test
        @DisplayName("Should return fallback when auth service returns 503 during login")
        public void returnFallbackWhenAuthServiceReturns503DuringLogin() throws Exception {
            authMock.stubLoginServiceUnavailable();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"test@example.com\", \"password\": \"password123\"}")
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("Should NOT trigger fallback for 401 during login (invalid credentials)")
        public void noFallbackFor401DuringLogin() throws Exception {
            authMock.stubLoginInvalidCredentials();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"test@example.com\", \"password\": \"wrongpassword\"}")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.message").value(containsString("Invalid"));
        }

        @Test
        @DisplayName("Should include fallback path in auth fallback response")
        public void includeFallbackPathInAuthResponse() throws Exception {
            authMock.stubLoginConnectionReset();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"test@example.com\", \"password\": \"password123\"}")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.path").isEqualTo("/fallback/auth-service");
        }
    }

    @Nested
    @DisplayName("Error Response Differentiation")
    class ErrorResponseDifferentiation {

        @Test
        @DisplayName("Should differentiate between client errors and service failures")
        public void differentiateBetweenClientAndServiceErrors() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubNotFound("non-existent");

            webTestClient.get()
                    .uri("/api/docs/folders/non-existent")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNotFound();
        }

        @Test
        @DisplayName("Should return 503 for server errors, not original 500")
        public void return503ForServerErrors() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            
            getDocsServiceMock().stubFor(
                WireMock.get(
                    WireMock.urlPathMatching("/api/docs/.*"))
                    .willReturn(WireMock.aResponse()
                        .withStatus(500)
                        .withBody("{\"error\": \"Internal error\"}"))
            );

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().is5xxServerError();
        }
    }

    @Nested
    @DisplayName("Partial Service Availability")
    class PartialServiceAvailability {

        @Test
        @DisplayName("Should work when auth is up but docs is down")
        public void authUpDocsDown() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubServiceDown();

            authMock.stubSuccessfulLogin(TEST_EMAIL, "new-token");
            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"" + TEST_EMAIL + "\", \"password\": \"password\"}")
                    .exchange()
                    .expectStatus().isOk();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("Should fail docs requests when auth is down (can't validate token)")
        public void authDownDocsUp() throws Exception {
            authMock.stubTokenValidationConnectionReset();
            docsMock.stubGetFolders(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);

            docsMock.verifyNoRequestsMade();
        }
    }
}
