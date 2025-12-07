package com.papairs.orchestration.e2e.routing;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Auth Protected Routes")
public class AuthProtectedRoutesTest extends AbstractE2ETest {

    @Nested
    @DisplayName("Logout Route")
    class LogoutRoute {

        @Test
        @DisplayName("Should allow logout with valid token")
        public void logoutWithValidToken() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            stubSuccessfulLogout();

            webTestClient.post()
                    .uri("/api/auth/logout")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(1);
        }

        @Test
        @DisplayName("Should return 401 when logout without token")
        public void logoutWithoutToken() throws Exception {
            webTestClient.post()
                    .uri("/api/auth/logout")
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(0);
        }

        @Test
        @DisplayName("Should return 401 when logout with invalid token")
        public void logoutWithInvalidToken() throws Exception {
            authMock.stubInvalidTokenValidationForAnyToken();

            webTestClient.post()
                    .uri("/api/auth/logout")
                    .header("Authorization", "Bearer invalid-token")
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(1);
        }

        @Test
        @DisplayName("Should return 503 when auth service down during logout")
        public void logoutWhenAuthServiceDown() throws Exception {
            authMock.stubTokenValidationConnectionReset();

            webTestClient.post()
                    .uri("/api/auth/logout")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);
        }

        @Test
        @DisplayName("Should pass X-User-Id to auth service on logout")
        public void logoutPassesUserIdHeader() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            stubSuccessfulLogout();

            webTestClient.post()
                    .uri("/api/auth/logout")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            verifyLogoutCalledWithUserId(TEST_USER_ID);
        }
    }

    @Nested
    @DisplayName("Generic Protected Auth Routes")
    class GenericProtectedRoutes {

        @Test
        @DisplayName("Should require authentication for any /api/auth/* route")
        public void anyAuthRouteRequiresToken() throws Exception {
            webTestClient.get()
                    .uri("/api/auth/test/me")
                    .exchange()
                    .expectStatus().isUnauthorized();

            webTestClient.post()
                    .uri("/api/auth/test/refresh")
                    .exchange()
                    .expectStatus().isUnauthorized();

            webTestClient.delete()
                    .uri("/api/auth/test/account")
                    .exchange()
                    .expectStatus().isUnauthorized();
        }

        @Test
        @DisplayName("Should allow access to protected auth routes with valid token")
        public void protectedAuthRoutesWithValidToken() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            stubAnyAuthServiceRequest();

            webTestClient.get()
                    .uri("/api/auth/me")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(1);
        }
    }

    private void stubSuccessfulLogout() {
        getAuthServiceMock().stubFor(
                WireMock.post(
                        WireMock.urlEqualTo("/api/auth/logout"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"message\": \"Logged out successfully\"}"))
        );
    }

    private void verifyLogoutCalledWithUserId(String userId) {
        getAuthServiceMock().verify(
                WireMock.postRequestedFor(
                        WireMock.urlEqualTo("/api/auth/logout"))
                        .withHeader("X-User-Id", 
                                WireMock.equalTo(userId))
        );
    }

    private void stubAnyAuthServiceRequest() {
        getAuthServiceMock().stubFor(
                WireMock.get(
                        WireMock.urlEqualTo("/api/auth/me"))
                        .willReturn(WireMock.aResponse()
                                .withStatus(200)
                                .withHeader("Content-Type", "application/json")
                                .withBody("{\"userId\": \"test-user\", \"email\": \"test@test.com\"}"))
        );
    }
}
