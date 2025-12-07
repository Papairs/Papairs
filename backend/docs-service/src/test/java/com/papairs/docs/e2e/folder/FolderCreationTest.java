package com.papairs.docs.e2e.folder;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Folder Creation")
public class FolderCreationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should create root-level folder when no parent specified")
    public void createRootLevelFolder() throws Exception {
        String requestBody = """
            {
                "name": "My Root Folder"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.folderId").exists())
                .andExpect(jsonPath("$.name").value("My Root Folder"))
                .andExpect(jsonPath("$.parentFolderId").isEmpty())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    @DisplayName("Should create nested folder when parent folder specified")
    public void createNestedFolder() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent Folder");

        String requestBody = """
            {
                "name": "Child Folder",
                "parentFolderId": "%s"
            }
            """.formatted(parentFolderId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Child Folder"))
                .andExpect(jsonPath("$.parentFolderId").value(parentFolderId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should create folder with trimmed title")
    public void createFolderWithTrimmedName() throws Exception {
        String requestBody = """
            {
                "name": "  Trimmed Name  "
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Trimmed Name"));
    }

    @Test
    @DisplayName("Should reject folder creation when name is blank")
    public void rejectBlankName() throws Exception {
        String requestBody = """
            {
                "name": ""
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Folder name is required")));
    }

    @Test
    @DisplayName("Should reject folder creation when name is only whitespace")
    public void rejectWhitespaceName() throws Exception {
        String requestBody = """
            {
                "name": "   "
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Folder name is required")));
    }

    @Test
    @DisplayName("Should reject folder creation when parent folder does not exist")
    public void createNestedFolderWithTrimmedFolderId() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent Folder");

        String requestBody = """
            {
                "name": "Child Folder",
                "parentFolderId": "   %s   "
            }
            """.formatted(parentFolderId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Child Folder"))
                .andExpect(jsonPath("$.parentFolderId").value(parentFolderId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should reject folder creation when parent folder does not exist")
    public void rejectNonExistentParentFolder() throws Exception {
        String nonExistentFolderId = "non-existent-folder-id";

        String requestBody = """
            {
                "name": "Child Folder",
                "parentFolderId": "%s"
            }
            """.formatted(nonExistentFolderId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should reject folder creation under parent owned by another user")
    public void rejectUnauthorizedParentFolder() throws Exception {
        String user2FolderId = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Folder");

        String requestBody = """
            {
                "name": "User 1's Subfolder",
                "parentFolderId": "%s"
            }
            """.formatted(user2FolderId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("don't have access")));
    }

    @Test
    @DisplayName("Should reject folder creation when X-User-Id header is missing")
    public void rejectMissingUserHeader() throws Exception {
        String requestBody = """
            {
                "name": "Test Folder"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should reject folder creation when name is null")
    public void rejectNullName() throws Exception {
        String requestBody = """
            {
                "name": null
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("Folder name is required")));
    }

    @Test
    @DisplayName("Should handle special characters in folder name")
    public void createFolderWithSpecialCharacters() throws Exception {
        String specialName = "Test & Folder <html> \"quotes\" 'single' 日本語 @#$%";

        String requestBody = """
            {
                "name": "%s"
            }
            """.formatted(specialName.replace("\"", "\\\""));

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(specialName));
    }

    @Test
    @DisplayName("Should allow multiple folders with same name at root level")
    public void allowDuplicateNamesAtRoot() throws Exception {
        String requestBody = """
            {
                "name": "Duplicate Name"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Duplicate Name')]").isArray())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    @DisplayName("Should allow multiple folders with same name under same parent")
    public void allowDuplicateNamesInSameParent() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");

        String requestBody = """
            {
                "name": "Duplicate Child",
                "parentFolderId": "%s"
            }
            """.formatted(parentFolderId);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/docs/folders/" + parentFolderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == 'Parent')]").isArray())
                .andExpect(jsonPath("$.children.length()").value(2));
    }

    @Test
    @DisplayName("Should create deeply nested folder hierarchy")
    public void createDeeplyNestedFolders() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);
        String level5 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 5", level4);

        mockMvc.perform(get("/api/docs/folders/" + level5 + "/path")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0].folderId").value(level1))
                .andExpect(jsonPath("$[4].folderId").value(level5));
    }

    @Test
    @DisplayName("Should create multiple child folders under same parent")
    public void createMultipleChildFolders() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");

        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", parentFolderId);
        String child2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", parentFolderId);
        String child3Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 3", parentFolderId);

        mockMvc.perform(get("/api/docs/folders/" + parentFolderId + "/children")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(child1Id)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(child2Id)).exists())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(child3Id)).exists());
    }

    @Test
    @DisplayName("Should appear in root folders list when created without parent")
    public void rootFolderAppearsInRootsList() throws Exception {
        String requestBody = """
            {
                "name": "New Root"
            }
            """;

        String response = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String folderId = objectMapper.readTree(response).get("folderId").asText();

        mockMvc.perform(get("/api/docs/folders/roots")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).exists());
    }

    @Test
    @DisplayName("Should appear in all folders list when created")
    public void folderAppearsInAllFoldersList() throws Exception {
        String requestBody = """
            {
                "name": "New Folder"
            }
            """;

        String response = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String folderId = objectMapper.readTree(response).get("folderId").asText();

        mockMvc.perform(get("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.folderId == '%s')]".formatted(folderId)).exists());
    }

    @Test
    @DisplayName("EDGE: Empty string parent folder ID creates root-level folder")
    public void emptyStringParentCreatesRootFolder() throws Exception {
        String requestBody = """
            {
                "name": "Root via Empty String",
                "parentFolderId": ""
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }
    
    @Test
    @DisplayName("EDGE: Whitespace-only parent folder ID creates root-level folder")
    public void whitespaceParentCreatesRootFolder() throws Exception {
        String requestBody = """
            {
                "name": "Root via Whitespace",
                "parentFolderId": "   "
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Null parent folder ID creates root-level folder")
    public void nullParentCreatesRootFolder() throws Exception {
        String requestBody = """
            {
                "name": "Root via Null",
                "parentFolderId": null
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());
    }

    @Test
    @DisplayName("EDGE: Creating folder does not affect existing folders")
    public void creatingFolderDoesNotAffectExisting() throws Exception {
        String existingFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Existing Folder");

        String existingResponse = mockMvc.perform(get("/api/docs/folders/" + existingFolderId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        fixtures.createFolderAsUser(TEST_USER_1_ID, "New Folder");

        String afterResponse = mockMvc.perform(get("/api/docs/folders/" + existingFolderId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assert existingResponse.equals(afterResponse) : "Existing folder should not be affected";
    }

    @Test
    @DisplayName("EDGE: Creating folder does not affect pages")
    public void creatingFolderDoesNotAffectPages() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Existing Page");

        fixtures.createFolderAsUser(TEST_USER_1_ID, "New Folder");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId));
    }

    @Test
    @DisplayName("EDGE: Can create folder after deleting one with same name")
    public void canCreateFolderAfterDeletingSameName() throws Exception {
        String name = "Reusable Name";
        String folderId1 = fixtures.createFolderAsUser(TEST_USER_1_ID, name);

        mockMvc.perform(delete("/api/docs/folders/" + folderId1)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        String folderId2 = fixtures.createFolderAsUser(TEST_USER_1_ID, name);

        assert !folderId1.equals(folderId2) : "New folder should have different ID";

        mockMvc.perform(get("/api/docs/folders/" + folderId2)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(name));
    }

    @Test
    @DisplayName("EDGE: Very long folder name is accepted")
    public void veryLongFolderNameAccepted() throws Exception {
        String longName = "a".repeat(255);

        String requestBody = """
            {
                "name": "%s"
            }
            """.formatted(longName);

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value(longName));
    }

    @Test
    @DisplayName("EDGE: Cannot create folder with empty user ID header")
    public void cannotCreateWithEmptyUserId() throws Exception {
        String requestBody = """
            {
                "name": "Test Folder"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, "")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Cannot create folder with whitespace-only user ID")
    public void cannotCreateWithWhitespaceUserId() throws Exception {
        String requestBody = """
            {
                "name": "Test Folder"
            }
            """;

        mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, "   ")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Different users can create folders with same name")
    public void differentUsersCanCreateSameNamedFolders() throws Exception {
        String sameName = "Common Name";

        String user1FolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, sameName);
        String user2FolderId = fixtures.createFolderAsUser(TEST_USER_2_ID, sameName);

        assert !user1FolderId.equals(user2FolderId) : "Different users should have different folder IDs";

        mockMvc.perform(get("/api/docs/folders/" + user1FolderId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));

        mockMvc.perform(get("/api/docs/folders/" + user2FolderId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_2_ID));
    }

    @Test
    @DisplayName("EDGE: New folder has zero page count initially")
    public void newFolderHasZeroPageCount() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Empty Folder");

        mockMvc.perform(get("/api/docs/folders/" + folderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageCount").value(0))
                .andExpect(jsonPath("$.childCount").value(0));
    }

    @Test
    @DisplayName("EDGE: New folder has no children initially")
    public void newFolderHasNoChildren() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");

        mockMvc.perform(get("/api/docs/folders/" + folderId + "/children")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("EDGE: Created folder ID is unique UUID format")
    public void folderIdIsUniqueUuid() throws Exception {
        String requestBody = """
            {
                "name": "UUID Test"
            }
            """;

        String response1 = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String response2 = mockMvc.perform(post("/api/docs/folders")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String folderId1 = objectMapper.readTree(response1).get("folderId").asText();
        String folderId2 = objectMapper.readTree(response2).get("folderId").asText();

        assert !folderId1.equals(folderId2) : "Folder IDs should be unique";
        assert folderId1.matches("[a-zA-Z0-9-_]+") : "Folder ID should be valid format";
    }
}
