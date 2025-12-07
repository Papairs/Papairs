package com.papairs.orchestration.e2e.routing;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.papairs.orchestration.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;

@DisplayName("Docs Service Gateway Routing")
public class DocsServiceRoutingTest extends AbstractE2ETest {

    @Nested
    @DisplayName("Folder Endpoints")
    class FolderEndpoints {

        @Test
        @DisplayName("Should route GET /api/docs/folders to docs service")
        public void routeGetFolders() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetFolders(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray();

            docsMock.verifyGetFoldersCalled(1);
        }

        @Test
        @DisplayName("Should route GET /api/docs/folders/roots to docs service")
        public void routeGetRootFolders() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetRootFolders(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders/roots")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should route GET /api/docs/folders/{id} to docs service")
        public void routeGetFolderById() throws Exception {
            String folderId = "folder-abc";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetFolder(folderId, TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/folders/" + folderId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.folderId").isEqualTo(folderId);
        }

        @Test
        @DisplayName("Should route POST /api/docs/folders to docs service")
        public void routeCreateFolder() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubCreateFolder(TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"name\": \"New Folder\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.folderId").exists()
                    .jsonPath("$.ownerId").isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should route DELETE /api/docs/folders/{id} to docs service")
        public void routeDeleteFolder() throws Exception {
            String folderId = "folder-to-delete";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubDeleteFolder(folderId, TEST_USER_ID);

            webTestClient.delete()
                    .uri("/api/docs/folders/" + folderId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNoContent();
        }
    }

    @Nested
    @DisplayName("Page Endpoints")
    class PageEndpoints {

        @Test
        @DisplayName("Should route GET /api/docs/pages to docs service")
        public void routeGetPages() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetPages(TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/pages")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$").isArray();

            docsMock.verifyGetPagesCalled(1);
        }

        @Test
        @DisplayName("Should route GET /api/docs/pages/{id} to docs service")
        public void routeGetPageById() throws Exception {
            String pageId = "page-xyz";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetPage(pageId, TEST_USER_ID);

            webTestClient.get()
                    .uri("/api/docs/pages/" + pageId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.pageId").isEqualTo(pageId);
        }

        @Test
        @DisplayName("Should route POST /api/docs/pages to docs service")
        public void routeCreatePage() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubCreatePage(TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/docs/pages")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"title\": \"New Page\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.pageId").exists();

            docsMock.verifyPostPagesCalled(1);
        }

        @Test
        @DisplayName("Should route PUT /api/docs/pages/{id} to docs service")
        public void routeUpdatePage() throws Exception {
            String pageId = "page-to-update";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubUpdatePage(pageId, TEST_USER_ID);

            webTestClient.put()
                    .uri("/api/docs/pages/" + pageId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"title\": \"Updated Title\", \"content\": \"Updated content\"}")
                    .exchange()
                    .expectStatus().isOk();
        }

        @Test
        @DisplayName("Should route DELETE /api/docs/pages/{id} to docs service")
        public void routeDeletePage() throws Exception {
            String pageId = "page-to-delete";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubDeletePage(pageId, TEST_USER_ID);

            webTestClient.delete()
                    .uri("/api/docs/pages/" + pageId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNoContent();
        }
    }

    @Nested
    @DisplayName("Error Response Preservation")
    class ErrorResponsePreservation {

        @Test
        @DisplayName("Should preserve 404 Not Found from docs service")
        public void preserve404NotFound() throws Exception {
            String nonExistentId = "non-existent-folder";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubNotFound(nonExistentId);

            webTestClient.get()
                    .uri("/api/docs/folders/" + nonExistentId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNotFound()
                    .expectBody()
                    .jsonPath("$.message").value(containsString("not found"));
        }

        @Test
        @DisplayName("Should preserve 403 Forbidden from docs service")
        public void preserve403Forbidden() throws Exception {
            String forbiddenUserId = "forbidden-user";
            authMock.stubValidTokenValidation(TEST_TOKEN, forbiddenUserId, TEST_EMAIL);
            docsMock.stubForbidden(forbiddenUserId);

            webTestClient.get()
                    .uri("/api/docs/folders/some-folder")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isForbidden()
                    .expectBody()
                    .jsonPath("$.message").value(containsString("denied"));
        }

        @Test
        @DisplayName("Should preserve 400 Bad Request from docs service")
        public void preserve400BadRequest() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            
            getDocsServiceMock().stubFor(
                WireMock.post(
                    WireMock.urlEqualTo("/api/docs/folders"))
                    .willReturn(WireMock.aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\": \"Folder name is required\"}"))
            );

            webTestClient.post()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"name\": \"\"}")
                    .exchange()
                    .expectStatus().isBadRequest()
                    .expectBody()
                    .jsonPath("$.message").value(containsString("required"));
        }
    }

    @Nested
    @DisplayName("Response Body Passthrough")
    class ResponseBodyPassthrough {

        @Test
        @DisplayName("Should pass through JSON array response from docs service")
        public void passthroughJsonArray() throws Exception {
            String jsonArrayResponse = """
                [
                    {"folderId": "f1", "name": "Folder 1", "ownerId": "%s"},
                    {"folderId": "f2", "name": "Folder 2", "ownerId": "%s"}
                ]
                """.formatted(TEST_USER_ID, TEST_USER_ID);

            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubGetFoldersWithData(TEST_USER_ID, jsonArrayResponse);

            webTestClient.get()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody()
                    .jsonPath("$.length()").isEqualTo(2)
                    .jsonPath("$[0].folderId").isEqualTo("f1")
                    .jsonPath("$[1].folderId").isEqualTo("f2");
        }

        @Test
        @DisplayName("Should pass through JSON object response from docs service")
        public void passthroughJsonObject() throws Exception {
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubCreateFolder(TEST_USER_ID);

            webTestClient.post()
                    .uri("/api/docs/folders")
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .header("Content-Type", "application/json")
                    .bodyValue("{\"name\": \"My Folder\"}")
                    .exchange()
                    .expectStatus().isCreated()
                    .expectBody()
                    .jsonPath("$.folderId").exists()
                    .jsonPath("$.name").exists()
                    .jsonPath("$.ownerId").isEqualTo(TEST_USER_ID);
        }

        @Test
        @DisplayName("Should pass through empty response for 204 No Content")
        public void passthroughEmptyResponse() throws Exception {
            String folderId = "folder-to-delete";
            authMock.stubValidTokenValidation(TEST_TOKEN, TEST_USER_ID, TEST_EMAIL);
            docsMock.stubDeleteFolder(folderId, TEST_USER_ID);

            webTestClient.delete()
                    .uri("/api/docs/folders/" + folderId)
                    .header("Authorization", "Bearer " + TEST_TOKEN)
                    .exchange()
                    .expectStatus().isNoContent()
                    .expectBody().isEmpty();
        }
    }
}
