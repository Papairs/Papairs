package com.papairs.orchestration.e2e.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.orchestration.dto.request.RegisterAccountRequest;
import org.springframework.test.web.reactive.server.WebTestClient;

public class TestFixtures {

    private final WebTestClient webTestClient;
    private final AuthServiceMock authMock;
    private final DocsServiceMock docsMock;
    private final ObjectMapper objectMapper;

    public TestFixtures(WebTestClient webTestClient, AuthServiceMock authMock, 
                       DocsServiceMock docsMock, ObjectMapper objectMapper) {
        this.webTestClient = webTestClient;
        this.authMock = authMock;
        this.docsMock = docsMock;
        this.objectMapper = objectMapper;
    }

    public void setupSuccessfulUserCreation(String email, String userId) throws Exception {
        authMock.stubSuccessfulRegistration(email, userId);
        docsMock.stubSuccessfulUserCreation(userId);
    }

    public void setupAuthSuccessDocsFailure(String email, String userId) throws Exception {
        authMock.stubSuccessfulRegistration(email, userId);
        docsMock.stubUserCreationFailure(userId);
        authMock.stubSuccessfulUserDeletion(userId);
    }

    public String registerAccount(String email, String password) throws Exception {
        RegisterAccountRequest request = new RegisterAccountRequest(email, password);
        
        String responseBody = webTestClient.post()
                .uri("/api/auth/register")
                .bodyValue(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        return objectMapper.readTree(responseBody).get("id").asText();
    }

    public WebTestClient.ResponseSpec attemptRegistration(String email, String password) {
        RegisterAccountRequest request = new RegisterAccountRequest(email, password);
        
        return webTestClient.post()
                .uri("/api/auth/register")
                .bodyValue(request)
                .exchange();
    }

    public void verifyAuthServiceCalled(int times) {
        authMock.verifyRegisterCalled(times);
    }

    public void verifyDocsServiceCalled(String userId, int times) {
        docsMock.verifyUserCreationCalled(userId, times);
    }

    public void verifyCompensationCalled(String userId, int times) {
        authMock.verifyDeleteCalled(userId, times);
    }
}
