package com.papairs.docs.e2e.page;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Page Creation")
public class PageCreationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should create page at root level when no folder specified")
    public void createPageAtRootLevel() throws Exception {
        String requestBody = """
            {
                "title": "My First Page"
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageId").exists())
                .andExpect(jsonPath("$.title").value("My First Page"))
                .andExpect(jsonPath("$.folderId").isEmpty())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should create page inside folder when folderId provided")
    public void createPageInFolder() throws Exception {
        String folderRequest = """
            {
                "name": "Work Projects"
            }
            """;

        String folderResponse = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(folderRequest))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String folderId = objectMapper.readTree(folderResponse).get("folderId").asText();

        String pageRequest = """
            {
                "title": "Project Plan",
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(pageRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").value(folderId))
                .andExpect(jsonPath("$.title").value("Project Plan"));
    }

    @Test
    @DisplayName("Should create page with trimmed title")
    public void createPageWithTrimmedTitle() throws Exception {
        String requestBody = """
            {
                "title": "  Trimmed Title  "
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("Trimmed Title"));
    }

    @Test
    @DisplayName("Should reject page creation when title is blank")
    public void rejectBlankTitle() throws Exception {
        String requestBody = """
            {
                "title": "   "
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is required")));
    }

    @Test
    @DisplayName("Should reject page creation when title is null")
    public void rejectNullTitle() throws Exception {
        String requestBody = """
            {
                "title": null
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Title is required")));
    }

    @Test
    @DisplayName("Should reject page creation when title exceeds 255 characters")
    public void rejectTitleTooLong() throws Exception {
        String longTitle = "a".repeat(256);

        String requestBody = """
            {
                "title": "%s"
            }
            """.formatted(longTitle);

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("255")));
    }

    @Test
    @DisplayName("Should reject page creation when folderId does not exist")
    public void rejectNonExistentFolder() throws Exception {
        String requestBody = """
            {
                "title": "Test Page",
                "folderId": "non-existent-folder-id"
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should reject page creation in folder owned by another user")
    public void rejectUnauthorizedFolderAccess() throws Exception {
        String folderRequest = """
            {
                "name": "User 1's Folder"
            }
            """;

        String folderResponse = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(folderRequest))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String user1FolderId = objectMapper.readTree(folderResponse).get("folderId").asText();

        String pageRequest = """
            {
                "title": "User 2's Page",
                "folderId": "%s"
            }
            """.formatted(user1FolderId);

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_3_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(pageRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));
    }

    @Test
    @DisplayName("Should reject page creation when user header is missing")
    public void rejectMissingUserHeader() throws Exception {
        String requestBody = """
            {
                "title": "Test Page"
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should handle special characters in title correctly")
    public void createPageWithSpecialCharacters() throws Exception {
        String specialTitle = "Test & Page <html> \"quotes\" 'single' 日本語";

        String requestBody = """
            {
                "title": "%s"
            }
            """.formatted(specialTitle.replace("\"", "\\\""));

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value(specialTitle));
    }

    @Test
    @DisplayName("EDGE: Empty string folderId should be treated as null")
    public void emptyStringFolderIdTreatedAsNull() throws Exception {
        String requestBody = """
            {
                "title": "Test",
                "folderId": ""
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Whitespace-only folderId should be treated as null")
    public void whitespaceOnlyFolderIdTreatedAsNull() throws Exception {
        String requestBody = """
            {
                "title": "Test",
                "folderId": "   "
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Null folderId in JSON should create root-level page")
    public void nullFolderIdCreatesRootPage() throws Exception {
        String requestBody = """
            {
                "title": "Test",
                "folderId": null
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Title with only whitespace should be rejected")
    public void titleWithOnlyWhitespaceRejected() throws Exception {
        String requestBody = """
            {
                "title": "     "
            }
            """;

        mockMvc.perform(post("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }
}
