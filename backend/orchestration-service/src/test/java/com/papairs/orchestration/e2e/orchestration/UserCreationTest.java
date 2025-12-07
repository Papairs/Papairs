package com.papairs.orchestration.e2e.orchestration;

import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("User Creation")
public class UserCreationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should successfully create user in both auth and docs services")
    public void createUserSuccessfully() throws Exception {
        fixtures.setupSuccessfulUserCreation(TEST_EMAIL, TEST_USER_ID);

        fixtures.attemptRegistration(TEST_EMAIL, TEST_PASSWORD)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(TEST_USER_ID)
                .jsonPath("$.email").isEqualTo(TEST_EMAIL)
                .jsonPath("$.emailVerified").isEqualTo(false)
                .jsonPath("$.active").isEqualTo(true);

        fixtures.verifyAuthServiceCalled(1);
        fixtures.verifyDocsServiceCalled(TEST_USER_ID, 1);
        fixtures.verifyCompensationCalled(TEST_USER_ID, 0);
    }

    @Test
    @DisplayName("Should validate email format before calling services")
    public void validateEmailFormat() throws Exception {
        String invalidEmail = "not-an-email";

        fixtures.attemptRegistration(invalidEmail, TEST_PASSWORD)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("email"));

        fixtures.verifyAuthServiceCalled(0);
    }

    @Test
    @DisplayName("Should validate password strength before calling services")
    public void validatePasswordStrength() throws Exception {
        String weakPassword = "weak";

        fixtures.attemptRegistration(TEST_EMAIL, weakPassword)
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").value(org.hamcrest.Matchers.containsString("password"));

        fixtures.verifyAuthServiceCalled(0);
    }
}
