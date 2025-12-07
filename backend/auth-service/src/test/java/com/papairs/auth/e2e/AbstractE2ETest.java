package com.papairs.auth.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.auth.e2e.fixture.TestFixtures;
import com.papairs.auth.repository.SessionRepository;
import com.papairs.auth.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class AbstractE2ETest {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected SessionRepository sessionRepository;

    protected static final String CONTENT_TYPE_JSON = "application/json";
    protected static final String AUTH_HEADER = "Authorization";

    protected static final String VALID_PASSWORD = "Test@1234";
    protected static final String VALID_PASSWORD_2 = "NewPass@5678";

    protected static final String TEST_EMAIL_1 = "testuser1@example.com";
    protected static final String TEST_EMAIL_2 = "testuser2@example.com";
    protected static final String TEST_EMAIL_3 = "testuser3@example.com";

    protected TestFixtures fixtures;

    @BeforeEach
    public void baseSetup() {
        fixtures = new TestFixtures(mockMvc, objectMapper, userRepository, sessionRepository, CONTENT_TYPE_JSON);
    }

    @AfterEach
    public void cleanup() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }

    protected String asJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }

    protected String bearerToken(String token) {
        return "Bearer " + token;
    }
}
