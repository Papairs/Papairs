package com.papairs.docs.e2e.fixture;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.papairs.docs.model.enums.MemberRole;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

public class TestFixtures {

    private final MockMvc mockMvc;
    private final ObjectMapper objectMapper;
    private final String userIdHeader;
    private final String contentTypeJson;

    public TestFixtures(MockMvc mockMvc, ObjectMapper objectMapper,
                        String userIdHeader, String contentTypeJson) {
        this.mockMvc = mockMvc;
        this.objectMapper = objectMapper;
        this.userIdHeader = userIdHeader;
        this.contentTypeJson = contentTypeJson;
    }

    public String createPageAsUser(String userId, String title) throws Exception {
        String requestBody = """
            {
                "title": "%s"
            }
            """.formatted(title);

        String response = mockMvc.perform(post("/api/docs/pages")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("pageId").asText();
    }

    public String createPageInFolder(String userId, String title, String folderId) throws Exception {
        String requestBody = """
            {
                "title": "%s",
                "folderId": "%s"
            }
            """.formatted(title, folderId);

        String response = mockMvc.perform(post("/api/docs/pages")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("pageId").asText();
    }

    public void verifyPageIsAtRoot(String pageId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());
    }

    public String createFolderAsUser(String userId, String name) throws Exception {
        return createFolderAsUser(userId, name, null);
    }

    public String createFolderAsUser(String userId, String name, String parentFolderId) throws Exception {
        String requestBody = parentFolderId == null
                ? """
                {
                    "name": "%s"
                }
                """.formatted(name)
                : """
                {
                    "name": "%s",
                    "parentFolderId": "%s"
                }
                """.formatted(name, parentFolderId);

        String response = mockMvc.perform(post("/api/docs/folders")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("folderId").asText();
    }

    public void addMemberToPage(String pageId, String ownerId, String memberId, MemberRole role) throws Exception {
        String requestBody = """
            {
                "userId": "%s",
                "role": "%s"
            }
            """.formatted(memberId, role);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(userIdHeader, ownerId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated());
    }

    public void removeMemberFromPage(String pageId, String ownerId, String memberId) throws Exception {
        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + memberId)
                        .header(userIdHeader, ownerId)
                        .contentType(contentTypeJson))
                .andExpect(status().isNoContent());
    }

    public void updateMemberRole(String pageId, String ownerId, String memberId, MemberRole newRole) throws Exception {
        String requestBody = """
            {
                "role": "%s"
            }
            """.formatted(newRole);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + memberId)
                        .header(userIdHeader, ownerId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    public void verifyMemberHasRole(String pageId, String memberId, MemberRole expectedRole) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(userIdHeader, memberId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')].role".formatted(memberId)).value(expectedRole.name()));
    }

    public void updatePageContent(String pageId, String userId, String content) throws Exception {
        String requestBody = """
            {
                "content": "%s"
            }
            """.formatted(content);

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    public void movePageToFolder(String pageId, String userId, String folderId) throws Exception {
        String requestBody = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isOk());
    }

    public void verifyPageExists(String pageId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk());
    }

    public void verifyPageDoesNotExist(String pageId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isNotFound());
    }

    public void verifyUserCanAccessPage(String pageId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk());
    }

    public void verifyUserCannotAccessPage(String pageId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isForbidden());
    }

    public void verifyUserCanEditPage(String pageId, String userId) throws Exception {
        String updateRequest = """
            {
                "content": "Test edit"
            }
            """;

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(updateRequest))
                .andExpect(status().isOk());
    }

    public void verifyUserCannotEditPage(String pageId, String userId) throws Exception {
        String updateRequest = """
            {
                "content": "Test edit"
            }
            """;

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(updateRequest))
                .andExpect(status().isForbidden());
    }

    public void verifyPageInUserList(String userId, String pageId) throws Exception {
        mockMvc.perform(get("/api/docs/pages")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pageId == '%s')]".formatted(pageId)).exists());
    }

    public void verifyPageNotInUserList(String userId, String pageId) throws Exception {
        mockMvc.perform(get("/api/docs/pages")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pageId == '%s')]".formatted(pageId)).doesNotExist());
    }

    public void verifyPageHasNoFolder(String pageId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());
    }

    public void verifyPageHasMembers(String pageId, String ownerId, int expectedCount) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(userIdHeader, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(expectedCount));
    }

    public String createFlashcardAsUser(String userId, String pageId, String question, String answer) throws Exception {
        String requestBody = """
            {
                "pageId": "%s",
                "question": "%s",
                "answer": "%s"
            }
            """.formatted(pageId, question, answer);

        String response = mockMvc.perform(post("/api/docs/flashcards")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("data").get("flashcardId").asText();
    }

    public String createFlashcardWithTags(String userId, String pageId, String question, String answer, String... tags) throws Exception {
        StringBuilder tagsJson = new StringBuilder("[");
        for (int i = 0; i < tags.length; i++) {
            tagsJson.append("\"").append(tags[i]).append("\"");
            if (i < tags.length - 1) {
                tagsJson.append(", ");
            }
        }
        tagsJson.append("]");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "%s",
                "answer": "%s",
                "tags": %s
            }
            """.formatted(pageId, question, answer, tagsJson.toString());

        String response = mockMvc.perform(post("/api/docs/flashcards")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).get("data").get("flashcardId").asText();
    }

    public void updateFlashcardLearnedStatus(String flashcardId, String userId, boolean learned) throws Exception {
        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .header(userIdHeader, userId)
                        .contentType(contentTypeJson)
                        .content(String.valueOf(learned)))
                .andExpect(status().isOk());
    }

    public void verifyFlashcardExists(String flashcardId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/flashcards")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.flashcardId == '%s')]".formatted(flashcardId)).exists());
    }

    public void verifyFlashcardDoesNotExist(String flashcardId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/flashcards")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.flashcardId == '%s')]".formatted(flashcardId)).doesNotExist());
    }

    public void verifyUserHasFlashcardCount(String userId, int expectedCount) throws Exception {
        mockMvc.perform(get("/api/docs/flashcards")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(expectedCount));
    }

    public void verifyPageHasFlashcardCount(String pageId, String userId, int expectedCount) throws Exception {
        mockMvc.perform(get("/api/docs/flashcards/page/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(expectedCount));
    }

    public void verifyFlashcardLearnedStatus(String flashcardId, String userId, boolean expectedLearned) throws Exception {
        mockMvc.perform(get("/api/docs/flashcards")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[?(@.flashcardId == '%s')].learned".formatted(flashcardId)).value(expectedLearned));
    }

    public void verifyFolderExists(String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk());
    }

    public void verifyFolderDoesNotExist(String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isNotFound());
    }

    public void verifyUserCanAccessFolder(String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk());
    }

    public void verifyUserCannotAccessFolder(String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isForbidden());
    }

    public void verifyFolderInRootList(String userId, String folderId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).exists());
    }

    public void verifyFolderNotInRootList(String userId, String folderId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).doesNotExist());
    }

    public void verifyFolderInAllFoldersList(String userId, String folderId) throws Exception {
        mockMvc.perform(get("/api/docs/folders")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).exists());
    }

    public void verifyFolderNotInAllFoldersList(String userId, String folderId) throws Exception {
        mockMvc.perform(get("/api/docs/folders")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).doesNotExist());
    }

    public void verifyFolderIsRoot(String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }

    public void verifyPageIsInFolder(String pageId, String folderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));
    }

    public void verifyFolderParent(String folderId, String expectedParentId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(expectedParentId));
    }

    public void verifyFolderHasNoParent(String folderId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, "ABC123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }

    public void verifyFolderInChildrenList(String parentFolderId, String childFolderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + parentFolderId + "/children")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(childFolderId)).exists());
    }

    public void verifyFolderNotInChildrenList(String parentFolderId, String childFolderId, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + parentFolderId + "/children")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(childFolderId)).doesNotExist());
    }

    public void verifyFolderChildCount(String folderId, int expectedCount, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId + "/tree")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childCount").value(expectedCount));
    }

    public void verifyFolderPageCount(String folderId, int expectedCount, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/folders/" + folderId + "/tree")
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageCount").value(expectedCount));
    }

    public void verifyPageTitle(String pageId, String expectedTitle, String userId) throws Exception {
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(expectedTitle));
    }

    public String getFolderParent(String folderId, String userId) throws Exception {
        String response = mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(userIdHeader, userId))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var node = objectMapper.readTree(response).get("parentFolderId");
        return node == null || node.isNull() ? null : node.asText();
    }
}