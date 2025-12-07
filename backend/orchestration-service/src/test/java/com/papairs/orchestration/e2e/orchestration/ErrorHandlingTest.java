package com.papairs.orchestration.e2e.orchestration;

import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;

import static org.hamcrest.Matchers.containsString;

@DisplayName("Error Handling")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ErrorHandlingTest extends AbstractE2ETest {

    @Test
    @Order(1)
    @DisplayName("Should preserve 409 Conflict when user already exists")
    public void preserveConflictStatus() throws Exception {
        authMock.stubUserAlreadyExists(TEST_EMAIL);

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.status").isEqualTo(409)
                .jsonPath("$.error").isEqualTo("Conflict")
                .jsonPath("$.message").value(containsString(TEST_EMAIL))
                .jsonPath("$.message").value(containsString("already exists"));

        fixtures.verifyDocsServiceCalled(TEST_USER_ID, 0);
    }

    @Test
    @Order(2)
    @DisplayName("Should map network errors to 503 Service Unavailable")
    public void mapNetworkErrorsToServiceUnavailable() throws Exception {
        authMock.stubServiceUnavailable();

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").value(containsString("unavailable"));
    }

    @Test
    @Order(3)
    @DisplayName("Should handle timeout errors")
    public void handleTimeoutErrors() throws Exception {
        authMock.stubRegistrationWithDelay(3000);

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(503);
    }
}
