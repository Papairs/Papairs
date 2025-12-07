package com.papairs.orchestration.e2e.routing;

import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@DisplayName("Auth Public Routes")
public class AuthPublicRoutesTest extends AbstractE2ETest {

    @Nested
    @DisplayName("Login Route")
    class LoginRoute {

        @Test
        @DisplayName("Should allow login without authentication token")
        public void loginWithoutToken() throws Exception {
            authMock.stubSuccessfulLogin(TEST_EMAIL, "new-session-token");

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"" + TEST_EMAIL + "\", \"password\": \"" + TEST_PASSWORD + "\"}")
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.token").exists();

            authMock.verifyTokenValidationCalled(0);
            authMock.verifyLoginCalled(1);
        }

        @Test
        @DisplayName("Should return 401 for invalid credentials")
        public void loginWithInvalidCredentials() throws Exception {
            authMock.stubLoginInvalidCredentials();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"wrong@test.com\", \"password\": \"wrongpassword\"}")
                    .exchange()
                    .expectStatus().isUnauthorized()
                    .expectBody()
                    .jsonPath("$.message").value(containsString("Invalid"));
        }

        @Test
        @DisplayName("Should return 503 when auth service is down")
        public void loginWhenAuthServiceDown() throws Exception {
            authMock.stubLoginConnectionReset();

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"test@test.com\", \"password\": \"password\"}")
                    .exchange()
                    .expectStatus().isEqualTo(503)
                    .expectBody()
                    .jsonPath("$.message").value(containsString("Authentication service"));
        }

        @Test
        @DisplayName("Should ignore Authorization header on login (public route)")
        public void loginIgnoresAuthHeader() throws Exception {
            authMock.stubSuccessfulLogin(TEST_EMAIL, "new-token");

            webTestClient.post()
                    .uri("/api/auth/login")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer some-random-token")
                    .bodyValue("{\"email\": \"" + TEST_EMAIL + "\", \"password\": \"" + TEST_PASSWORD + "\"}")
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(0);
        }
    }

    @Nested
    @DisplayName("Register Route")
    class RegisterRoute {

        @Test
        @DisplayName("Should allow registration without authentication token")
        public void registerWithoutToken() throws Exception {
            fixtures.setupSuccessfulUserCreation(TEST_EMAIL, TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/auth/register")
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"email\": \"" + TEST_EMAIL + "\", \"password\": \"" + TEST_PASSWORD + "\"}")
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(0);
            authMock.verifyRegisterCalled(1);
        }

        @Test
        @DisplayName("Should ignore Authorization header on register (public route)")
        public void registerIgnoresAuthHeader() throws Exception {
            fixtures.setupSuccessfulUserCreation(TEST_EMAIL, TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/auth/register")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer some-random-token")
                    .bodyValue("{\"email\": \"" + TEST_EMAIL + "\", \"password\": \"" + TEST_PASSWORD + "\"}")
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(0);
        }
    }
}
