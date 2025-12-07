package com.papairs.orchestration.e2e.fixture;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class DocsServiceMock {

    private final WireMockServer wireMockServer;

    public DocsServiceMock(WireMockServer wireMockServer) {
        this.wireMockServer = wireMockServer;
    }

    public void stubSuccessfulUserCreation(String userId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/docs/users"))
                .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                .willReturn(aResponse()
                        .withStatus(201)));
    }

    public void stubUserCreationFailure(String userId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/docs/users"))
                .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"error\": \"Service unavailable\"}")));
    }

    public void stubUserCreationWithDelay(String userId, int delayMs) {
        wireMockServer.stubFor(post(urlEqualTo("/api/docs/users"))
                .withRequestBody(matchingJsonPath("$.userId", equalTo(userId)))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withFixedDelay(delayMs)));
    }

    public void stubGetFolders(String userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/folders"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
    }

    public void stubGetFoldersWithData(String userId, String jsonResponse) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/folders"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(jsonResponse)));
    }

    public void stubGetRootFolders(String userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/folders/roots"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
    }

    public void stubCreateFolder(String userId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/docs/folders"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"folderId\": \"folder-123\", \"name\": \"Test Folder\", \"ownerId\": \"" + userId + "\"}")));
    }

    public void stubGetFolder(String folderId, String userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/folders/" + folderId))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"folderId\": \"" + folderId + "\", \"name\": \"Test Folder\", \"ownerId\": \"" + userId + "\"}")));
    }

    public void stubDeleteFolder(String folderId, String userId) {
        wireMockServer.stubFor(delete(urlEqualTo("/api/docs/folders/" + folderId))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

    public void stubGetPages(String userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/pages"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("[]")));
    }

    public void stubCreatePage(String userId) {
        wireMockServer.stubFor(post(urlEqualTo("/api/docs/pages"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageId\": \"page-123\", \"title\": \"Test Page\", \"ownerId\": \"" + userId + "\"}")));
    }

    public void stubGetPage(String pageId, String userId) {
        wireMockServer.stubFor(get(urlEqualTo("/api/docs/pages/" + pageId))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageId\": \"" + pageId + "\", \"title\": \"Test Page\", \"ownerId\": \"" + userId + "\"}")));
    }

    public void stubUpdatePage(String pageId, String userId) {
        wireMockServer.stubFor(put(urlEqualTo("/api/docs/pages/" + pageId))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"pageId\": \"" + pageId + "\", \"title\": \"Updated Page\", \"ownerId\": \"" + userId + "\"}")));
    }

    public void stubDeletePage(String pageId, String userId) {
        wireMockServer.stubFor(delete(urlEqualTo("/api/docs/pages/" + pageId))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(204)));
    }

    public void stubNotFound(String resourceId) {
        wireMockServer.stubFor(get(urlPathMatching("/api/docs/.*/" + resourceId))
                .willReturn(aResponse()
                        .withStatus(404)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Resource not found: " + resourceId + "\"}")));
    }

    public void stubForbidden(String userId) {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(403)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Access denied\"}")));
    }

    public void stubServiceUnavailable() {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .willReturn(aResponse()
                        .withStatus(503)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Service unavailable\"}")));
    }

    public void stubServiceDown() {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));
    }

    public void stubServiceTimeout(int delayMs) {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withFixedDelay(delayMs)));
    }

    public void stubAnyDocsRequest() {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
    }

    public void stubAnyDocsRequestWithUserIdHeader(String userId) {
        wireMockServer.stubFor(any(urlPathMatching("/api/docs/.*"))
                .withHeader("X-User-Id", equalTo(userId))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{}")));
    }

    public void verifyUserCreationCalled(String userId, int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/docs/users"))
                .withRequestBody(matchingJsonPath("$.userId", equalTo(userId))));
    }

    public void verifyNoRequestsMade() {
        wireMockServer.verify(0, anyRequestedFor(anyUrl()));
    }

    public void verifyRequestMade(int times) {
        wireMockServer.verify(times, anyRequestedFor(anyUrl()));
    }

    public void verifyRequestContainedHeader(String headerName, String headerValue) {
        wireMockServer.verify(anyRequestedFor(anyUrl())
                .withHeader(headerName, equalTo(headerValue)));
    }

    public void verifyRequestDidNotContainHeader(String headerName, String headerValue) {
        wireMockServer.verify(0, anyRequestedFor(anyUrl())
                .withHeader(headerName, equalTo(headerValue)));
    }

    public void verifyGetFoldersCalled(int times) {
        wireMockServer.verify(times, getRequestedFor(urlEqualTo("/api/docs/folders")));
    }

    public void verifyPostFoldersCalled(int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/docs/folders")));
    }

    public void verifyGetPagesCalled(int times) {
        wireMockServer.verify(times, getRequestedFor(urlEqualTo("/api/docs/pages")));
    }

    public void verifyPostPagesCalled(int times) {
        wireMockServer.verify(times, postRequestedFor(urlEqualTo("/api/docs/pages")));
    }

    public void reset() {
        wireMockServer.resetAll();
    }
}
