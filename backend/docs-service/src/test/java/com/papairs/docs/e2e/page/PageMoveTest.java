package com.papairs.docs.e2e.page;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Page Move")
public class PageMoveTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to move page to different folder")
    public void ownerCanMovePageToDifferentFolder() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source Folder");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page to Move", sourceFolderId);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.folderId").value(targetFolderId));

        fixtures.verifyPageIsInFolder(pageId, targetFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow owner to move page from folder to root")
    public void ownerCanMovePageToRoot() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page to Root", folderId);

        String moveRequest = """
            {
                "folderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow owner to move page from root to folder")
    public void ownerCanMovePageFromRootToFolder() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Root Page");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target Folder");

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(targetFolderId));

        fixtures.verifyPageIsInFolder(pageId, targetFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny move by non-member")
    public void nonMemberCannotMovePage() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));
    }

    @Test
    @DisplayName("Should deny move by VIEWER member")
    public void viewerCannotMovePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny move by EDITOR member")
    public void editorCannotMovePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny move to folder owned by another user")
    public void cannotMoveToUnauthorizedFolder() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");
        String user1FolderId = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Folder");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(user1FolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));
    }

    @Test
    @DisplayName("Should return 404 when moving non-existent page")
    public void movingNonExistentPageReturns404() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String nonExistentPageId = "non-existent-page-id";

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + nonExistentPageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when moving to non-existent folder")
    public void movingToNonExistentFolderReturns404() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String nonExistentFolderId = "non-existent-folder-id";

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(nonExistentFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("EDGE: Moving page to a non-trimmed folderId still moves it")
    public void trimmedFolderIdMoves() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        String moveRequest = """
            {
                "folderId": "  %s   "
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));

        fixtures.verifyPageIsInFolder(pageId, folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Moving page to empty string folderId moves to root")
    public void emptyStringFolderIdMovesToRoot() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        String moveRequest = """
            {
                "folderId": ""
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page to whitespace-only folderId moves to root")
    public void whitespaceFolderIdMovesToRoot() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        String moveRequest = """
            {
                "folderId": "   "
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page to null folderId moves to root")
    public void nullFolderIdMovesToRoot() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        String moveRequest = """
            {
                "folderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page to same folder succeeds idempotently")
    public void movingToSameFolderSucceeds() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));

        fixtures.verifyPageIsInFolder(pageId, folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving root-level page to root succeeds idempotently")
    public void movingRootPageToRootSucceeds() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Root Page");

        String moveRequest = """
            {
                "folderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").isEmpty());

        fixtures.verifyPageIsAtRoot(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page updates source folder page count")
    public void movingPageUpdatesSourceFolderCount() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", sourceFolderId);

        fixtures.verifyFolderPageCount(sourceFolderId, 1, TEST_USER_1_ID);
        fixtures.verifyFolderPageCount(targetFolderId, 0, TEST_USER_1_ID);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderPageCount(sourceFolderId, 0, TEST_USER_1_ID);
        fixtures.verifyFolderPageCount(targetFolderId, 1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page does not affect page members")
    public void movingPagePreservesMembers() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Shared Page", sourceFolderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page does not affect page content")
    public void movingPagePreservesContent() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page with Content", sourceFolderId);

        String content = "Important content that should not be lost";
        fixtures.updatePageContent(pageId, TEST_USER_1_ID, content);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @DisplayName("EDGE: Moving page does not affect page title")
    public void movingPagePreservesTitle() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String title = "Important Title";
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, title, sourceFolderId);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    @DisplayName("EDGE: Can move multiple pages to same folder")
    public void canMoveMultiplePagesToSameFolder() throws Exception {
        String sourceFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Source");
        String targetFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");
        String page1Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", sourceFolderId);
        String page2Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", sourceFolderId);
        String page3Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 3", sourceFolderId);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(targetFolderId);

        mockMvc.perform(patch("/api/docs/pages/" + page1Id + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/docs/pages/" + page2Id + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/docs/pages/" + page3Id + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderPageCount(sourceFolderId, 0, TEST_USER_1_ID);
        fixtures.verifyFolderPageCount(targetFolderId, 3, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving page to deeply nested folder succeeds")
    public void canMovePageToDeeplyNestedFolder() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);

        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(level4);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(level4));
    }

    @Test
    @DisplayName("EDGE: Pages with same title can exist in different folders")
    public void pagesWithSameTitleInDifferentFolders() throws Exception {
        String folder1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 1");
        String folder2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 2");

        String page1Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Duplicate Title", folder1Id);
        String page2Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Duplicate Title", folder1Id);

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folder2Id);

        mockMvc.perform(patch("/api/docs/pages/" + page2Id + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(page1Id, folder1Id, TEST_USER_1_ID);
        fixtures.verifyPageIsInFolder(page2Id, folder2Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot move page with empty user ID header")
    public void cannotMoveWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, "")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Cannot move page with whitespace-only user ID")
    public void cannotMoveWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");

        String moveRequest = """
            {
                "folderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, "   ")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Moving page chain maintains consistency")
    public void movingPageChainMaintainsConsistency() throws Exception {
        String folder1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 1");
        String folder2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 2");
        String folder3Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 3");

        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Traveling Page", folder1Id);

        fixtures.verifyPageIsInFolder(pageId, folder1Id, TEST_USER_1_ID);

        String moveToFolder2 = """
            {
                "folderId": "%s"
            }
            """.formatted(folder2Id);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToFolder2))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(pageId, folder2Id, TEST_USER_1_ID);

        String moveToFolder3 = """
            {
                "folderId": "%s"
            }
            """.formatted(folder3Id);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToFolder3))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(pageId, folder3Id, TEST_USER_1_ID);
    }
}
