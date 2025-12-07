package com.papairs.orchestration.e2e;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.papairs.orchestration.e2e.fixture.AuthServiceMock;
import com.papairs.orchestration.e2e.fixture.DocsServiceMock;
import com.papairs.orchestration.e2e.fixture.TestFixtures;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

/**
 * Base class for E2E tests.
 * 
 * IMPORTANT: WireMock servers are started ONCE for ALL test classes and shared.
 * Each test method gets a fresh set of stubs via @BeforeEach.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)  // Share instance across test methods
public abstract class AbstractE2ETest {

    @Autowired
    protected WebTestClient webTestClient;

    @Autowired
    protected ObjectMapper objectMapper;

    private static WireMockServer authServiceMock;
    private static WireMockServer docsServiceMock;
    private static boolean serversStarted = false;

    protected AuthServiceMock authMock;
    protected DocsServiceMock docsMock;
    protected TestFixtures fixtures;

    protected static final String TEST_USER_ID = "test-user-123";
    protected static final String TEST_EMAIL = "test@example.com";
    protected static final String TEST_PASSWORD = "SecurePass123!";
    protected static final String TEST_TOKEN = "mock-session-token-12345";

    @BeforeAll
    public static void startWireMockOnce() {
        if (!serversStarted) {
            authServiceMock = new WireMockServer(wireMockConfig()
                    .dynamicPort()
                    .stubRequestLoggingDisabled(false));
            docsServiceMock = new WireMockServer(wireMockConfig()
                    .dynamicPort()
                    .stubRequestLoggingDisabled(false));
            
            authServiceMock.start();
            docsServiceMock.start();
            serversStarted = true;
            
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (authServiceMock != null && authServiceMock.isRunning()) {
                    authServiceMock.stop();
                }
                if (docsServiceMock != null && docsServiceMock.isRunning()) {
                    docsServiceMock.stop();
                }
            }));
        }
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        startWireMockOnce();
        
        registry.add("services.auth-service.url",
            () -> "http://localhost:" + authServiceMock.port());
        registry.add("services.docs-service.url",
            () -> "http://localhost:" + docsServiceMock.port());
    }

    @BeforeEach
    public void setUp() {
        authServiceMock.resetAll();
        docsServiceMock.resetAll();

        authMock = new AuthServiceMock(authServiceMock, objectMapper);
        docsMock = new DocsServiceMock(docsServiceMock);
        fixtures = new TestFixtures(webTestClient, authMock, docsMock, objectMapper);
    }

    protected String asJson(Object object) throws Exception {
        return objectMapper.writeValueAsString(object);
    }
    
    protected static WireMockServer getAuthServiceMock() {
        return authServiceMock;
    }
    
    protected static WireMockServer getDocsServiceMock() {
        return docsServiceMock;
    }
}
