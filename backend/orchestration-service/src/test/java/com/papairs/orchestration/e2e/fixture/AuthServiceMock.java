package com.papairs.orchestration.e2e.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import com.papairs.orchestration.dto.response.RegisterResponse;
import com.papairs.orchestration.dto.response.ValidationResponse;
import com.papairs.orchestration.dto.response.UserResponse;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class AuthServiceMock {

    private final WireMockServer wireMockServer;
    private final ObjectMapper objectMapper;

    public AuthServiceMock(WireMockServer wireMockServer, ObjectMapper objectMapper) {
        this.wireMockServer = wireMockServer;
        this.objectMapper = objectMapper;
    }

    public void stubSuccessfulRegistration(String email, String userId) throws Exception {
        UserResponse user = createUserResponse(userId, email);
        RegisterResponse response = new RegisterResponse("User registered successfully", user);

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    public void stubUserAlreadyExists(String email) throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "User with email " + email + " already exists");

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse()
                        .withStatus(409)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(errorResponse))));
    }

    public void stubServiceUnavailable() {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": false, \"message\": \"Service temporarily unavailable\"}")));
    }

    public void stubRegistrationWithDelay(int delayMs) {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/register"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(delayMs)));
    }

    public void stubValidTokenValidation(String token, String userId, String email) throws Exception {
        ValidationResponse response = new ValidationResponse(userId);

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .withHeader("Authorization", equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    public void stubValidTokenValidationForAnyToken(String userId) throws Exception {
        ValidationResponse response = new ValidationResponse(userId);

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    public void stubInvalidTokenValidation(String token) throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Invalid or expired token");

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .withHeader("Authorization", equalTo("Bearer " + token))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(errorResponse))));
    }

    public void stubInvalidTokenValidationForAnyToken() throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Invalid or expired token");

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .withHeader("Authorization", matching("Bearer .+"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(errorResponse))));
    }

    public void stubTokenValidationServiceUnavailable() {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": false, \"message\": \"Service temporarily unavailable\"}")));
    }

    public void stubTokenValidationTimeout(int delayMs) {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delayMs)));
    }

    public void stubTokenValidationConnectionReset() {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/validate"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public void stubSuccessfulUserDeletion(String userId) {
        wireMockServer.stubFor(delete(urlMatching("/api/auth/" + userId))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

    public void stubUserDeletionFailure(String userId) {
        wireMockServer.stubFor(delete(urlMatching("/api/auth/" + userId))
                .willReturn(aResponse()
                        .withStatus(500)));
    }

    public void stubSuccessfulLogin(String email, String token) throws Exception {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("token", token);
        response.put("message", "Login successful");

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(response))));
    }

    public void stubLoginInvalidCredentials() throws Exception {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("success", false);
        errorResponse.put("message", "Invalid email or password");

        wireMockServer.stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse()
                        .withStatus(401)
                        .withHeader("Content-Type", "application/json")
                        .withBody(objectMapper.writeValueAsString(errorResponse))));
    }

    public void stubLoginServiceUnavailable() {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"success\": false, \"message\": \"Service temporarily unavailable\"}")));
    }

    public void stubLoginConnectionReset() {
        wireMockServer.stubFor(post(urlEqualTo("/api/auth/login"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public void verifyRegisterCalled(int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/auth/register")));
    }

    public void verifyDeleteCalled(String userId, int times) {
        wireMockServer.verify(times, deleteRequestedFor(urlMatching("/api/auth/" + userId)));
    }

    public void verifyTokenValidationCalled(int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/auth/validate")));
    }

    public void verifyTokenValidationCalledWithToken(String token) {
        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/auth/validate"))
                .withHeader("Authorization", equalTo("Bearer " + token)));
    }

    public void verifyLoginCalled(int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/auth/login")));
    }

    private UserResponse createUserResponse(String userId, String email) {
        return new UserResponse(
                userId,
                email,
                false,
                true,
                LocalDateTime.now(),
                null
        );
    }

    public void reset() {
        wireMockServer.resetAll();
    }
}
