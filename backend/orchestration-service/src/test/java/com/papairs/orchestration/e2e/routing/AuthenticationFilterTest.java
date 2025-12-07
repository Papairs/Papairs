package com.papairs.orchestration.e2e.routing;

import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Authentication Gateway Filter")
public class AuthenticationFilterTest extends AbstractE2ETest {

    @Nested
    @DisplayName("Token Validation")
    class TokenValidation {

        @Test
        @DisplayName("Should allow request when token is valid")
        public void allowRequestWithValidToken() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetFolders(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            authMock.verifyTokenValidationCalled(1);
            docsMock.verifyGetFoldersCalled(1);
        }

        @Test
        @DisplayName("Should return 401 when Authorization header is missing")
        public void return401WhenNoAuthHeader() throws Exception {
            webTestClient.get()
                    .uri("/api/docs/folders")
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(0);
            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 401 when Authorization header is empty")
        public void return401WhenAuthHeaderEmpty() throws Exception {
            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "")
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(0);
            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 401 when Bearer prefix is missing")
        public void return401WhenBearerPrefixMissing() throws Exception {
            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", TEST_TOKEN)
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(0);
            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 401 when Authorization uses wrong scheme")
        public void return401WhenWrongAuthScheme() throws Exception {
            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Basic " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(0);
            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 401 when token is invalid")
        public void return401WhenTokenInvalid() throws Exception {
            authMock.stubInvalidTokenValidationForAnyToken();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer invalid-token-xyz")
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(1);
            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 401 when token is expired")
        public void return401WhenTokenExpired() throws Exception {
            String expiredToken = "expired-token-123";
            authMock.stubInvalidTokenValidation(expiredToken);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + expiredToken)
                    .exchange()
                    .expectStatus().isUnauthorized();

            authMock.verifyTokenValidationCalled(1);
            docsMock.verifyNoRequestsMade();
        }
    }

    @Nested
    @DisplayName("User ID Header Injection")
    class UserIdHeaderInjection {

        @Test
        @DisplayName("Should add X-User-Id header to downstream request")
        public void addUserIdHeader() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubAnyDocsRequestWithUserIdHeader(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            docsMock.verifyRequestContainedHeader("X-User-Id", TEST_USER_ID);
        }

        @Test
        @DisplayName("Should strip client-provided X-User-Id header to prevent spoofing")
        public void preventUserIdSpoofing() throws Exception {
            String spoofedUserId = "ATTACKER-SPOOFED-ID";
            
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubAnyDocsRequestWithUserIdHeader(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("X-User-Id", spoofedUserId)
                    .exchange()
                    .expectStatus().isOk();

            docsMock.verifyRequestContainedHeader("X-User-Id", TEST_USER_ID);
            docsMock.verifyRequestDidNotContainHeader("X-User-Id", spoofedUserId);
        }

        @Test
        @DisplayName("Should use different user IDs for different tokens")
        public void differentTokensDifferentUsers() throws Exception {
            String token1 = "token-user-1";
            String token2 = "token-user-2";
            String userId1 = "user-001";
            String userId2 = "user-002";

            authMock.stubValidTokenValidation(token1, userId1, "user1@test.com");
            authMock.stubValidTokenValidation(token2, userId2, "user2@test.com");
            docsMock.stubGetFolders(userId1);
            docsMock.stubGetFolders(userId2);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + token1)
                    .exchange()
                    .expectStatus().isOk();

            docsMock.verifyRequestContainedHeader("X-User-Id", userId1);

            docsMock.reset();
            docsMock.stubGetFolders(userId2);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + token2)
                    .exchange()
                    .expectStatus().isOk();

            docsMock.verifyRequestContainedHeader("X-User-Id", userId2);
        }
    }

    @Nested
    @DisplayName("Auth Service Availability")
    class AuthServiceAvailability {

        @Test
        @DisplayName("Should return 503 when auth service is unavailable")
        public void return503WhenAuthServiceDown() throws Exception {
            authMock.stubTokenValidationServiceUnavailable();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);

            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 503 when auth service connection resets")
        public void return503WhenAuthServiceConnectionReset() throws Exception {
            authMock.stubTokenValidationConnectionReset();

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);

            docsMock.verifyNoRequestsMade();
        }

        @Test
        @DisplayName("Should return 503 when auth service times out")
        public void return503WhenAuthServiceTimesOut() throws Exception {
            // Stub delay (5s) > service timeout (2s from application-test.yml) 
            // Service should timeout and return 503 before stub responds
            authMock.stubTokenValidationTimeout(5000);

            webTestClient
                    .mutate()
                    .responseTimeout(java.time.Duration.ofSeconds(10))
                    .build()
                    .get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isEqualTo(503);

            docsMock.verifyNoRequestsMade();
        }
    }

    @Nested
    @DisplayName("HTTP Methods")
    class HttpMethods {

        @Test
        @DisplayName("Should authenticate GET requests")
        public void authenticateGetRequests() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetFolders(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();

            docsMock.verifyGetFoldersCalled(1);
        }

        @Test
        @DisplayName("Should authenticate POST requests")
        public void authenticatePostRequests() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubCreateFolder(TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"name\": \"Test Folder\"}")
                    .exchange()
                    .expectStatus().isCreated();

            docsMock.verifyPostFoldersCalled(1);
        }

        @Test
        @DisplayName("Should authenticate PUT requests")
        public void authenticatePutRequests() throws Exception {
            String pageId = "page-123";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubUpdatePage(pageId, TEST_USER_ID);

            webTestClient.put()
                    .uri("/api/docs/pages/" + pageId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"title\": \"Updated Title\"}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should authenticate DELETE requests")
        public void authenticateDeleteRequests() throws Exception {
            String folderId = "folder-123";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubDeleteFolder(folderId, TEST_USER_ID);

            webTestClient.delete()
                    .uri("/api/docs/folders/" + folderId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNoContent();
        }
    }
}
