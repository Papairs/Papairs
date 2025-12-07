package com.papairs.orchestration.e2e.orchestration;

import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@DisplayName("Compensation")
public class CompensationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should rollback auth user when docs service fails")
    public void compensateWhenDocsServiceFails() throws Exception {
        fixtures.setupAuthSuccessDocsFailure(TEST_EMAIL, TEST_USER_ID);

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(503)
                .expectBody()
                .jsonPath("$.status").isEqualTo(503)
                .jsonPath("$.message").value(containsString("try again later"));

        fixtures.verifyAuthServiceCalled(1);
        fixtures.verifyDocsServiceCalled(TEST_USER_ID, 1);
        fixtures.verifyCompensationCalled(TEST_USER_ID, 1);
    }

    @Test
    @DisplayName("Should return error even if compensation fails")
    public void handleCompensationFailureGracefully() throws Exception {
        authMock.stubSuccessfulRegistration(TEST_EMAIL, TEST_USER_ID);
        docsMock.stubUserCreationFailure(TEST_USER_ID);
        authMock.stubUserDeletionFailure(TEST_USER_ID);

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(503);

        fixtures.verifyCompensationCalled(TEST_USER_ID, 1);
    }

    @Test
    @DisplayName("Should not trigger compensation when auth service fails")
    public void noCompensationWhenAuthFails() throws Exception {
        authMock.stubServiceUnavailable();

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isEqualTo(503);

        fixtures.verifyAuthServiceCalled(1);
        fixtures.verifyDocsServiceCalled(TEST_USER_ID, 0);
        fixtures.verifyCompensationCalled(TEST_USER_ID, 0);
    }
}
