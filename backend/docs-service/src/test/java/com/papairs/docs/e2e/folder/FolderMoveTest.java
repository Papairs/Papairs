package com.papairs.docs.e2e.folder;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Folder Move")
public class FolderMoveTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to move folder to different parent")
    public void ownerCanMoveFolderToNewParent() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder to Move", oldParent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderToMove))
                .andExpect(jsonPath("$.parentFolderId").value(newParent));

        fixtures.verifyFolderParent(folderToMove, newParent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow owner to move folder from nested to root level")
    public void ownerCanMoveFolderToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Nested Folder", parent);

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(folderToMove, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow owner to move folder from root to nested level")
    public void ownerCanMoveFolderFromRootToParent() throws Exception {
        String rootFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root Folder");
        String targetParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target Parent");

        fixtures.verifyFolderIsRoot(rootFolder, TEST_USER_1_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(targetParent);

        mockMvc.perform(patch("/api/docs/folders/" + rootFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(targetParent));

        fixtures.verifyFolderParent(rootFolder, targetParent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny move by non-owner")
    public void nonOwnerCannotMoveFolder() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder", parent);
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));

        fixtures.verifyFolderParent(folderToMove, parent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny moving folder into itself")
    public void cannotMoveFolderIntoItself() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Self Reference Folder");

        String originalParent = fixtures.getFolderParent(folderId, TEST_USER_1_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(folderId);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("itself")));

        String currentParent = fixtures.getFolderParent(folderId, TEST_USER_1_ID);
        assert Objects.equals(originalParent, currentParent);
    }

    @Test
    @DisplayName("Should deny moving folder into its direct child")
    public void cannotMoveFolderIntoDirectChild() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(child);

        mockMvc.perform(patch("/api/docs/folders/" + parent + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("descendant")));

        fixtures.verifyFolderIsRoot(parent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny moving folder into its grandchild")
    public void cannotMoveFolderIntoGrandchild() throws Exception {
        String grandparent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandparent");
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent", grandparent);
        String child = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(child);

        mockMvc.perform(patch("/api/docs/folders/" + grandparent + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("descendant")));

        fixtures.verifyFolderIsRoot(grandparent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny moving folder into deeply nested descendant")
    public void cannotMoveFolderIntoDeeplyNestedDescendant() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);
        String level5 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 5", level4);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(level5);

        mockMvc.perform(patch("/api/docs/folders/" + level1 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("descendant")));

        fixtures.verifyFolderIsRoot(level1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny moving folder to parent owned by another user")
    public void cannotMoveToUnauthorizedParent() throws Exception {
        String user1Folder = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");
        String user2Parent = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Parent");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(user2Parent);

        mockMvc.perform(patch("/api/docs/folders/" + user1Folder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));

        fixtures.verifyFolderIsRoot(user1Folder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 404 when moving non-existent folder")
    public void movingNonExistentFolderReturns404() throws Exception {
        String nonExistentFolderId = "non-existent-folder-id";
        String targetParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(targetParent);

        mockMvc.perform(patch("/api/docs/folders/" + nonExistentFolderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when moving to non-existent parent")
    public void movingToNonExistentParentReturns404() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String nonExistentParentId = "non-existent-parent-id";

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(nonExistentParentId);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String targetParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(targetParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Empty string parentFolderId moves folder to root")
    public void emptyStringParentMovesToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", parent);

        String moveRequest = """
            {
                "parentFolderId": ""
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(folderToMove, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Whitespace-only parentFolderId moves folder to root")
    public void whitespaceParentMovesToRoot() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", parent);

        String moveRequest = """
            {
                "parentFolderId": "   "
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(folderToMove, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder to same parent succeeds idempotently")
    public void movingToSameParentSucceeds() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", parent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(parent));

        fixtures.verifyFolderParent(folderToMove, parent, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving root folder to root succeeds idempotently")
    public void movingRootToRootSucceeds() throws Exception {
        String rootFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root Folder");

        String moveRequest = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + rootFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").isEmpty());

        fixtures.verifyFolderIsRoot(rootFolder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder preserves all subfolders")
    public void movingFolderPreservesSubfolders() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);
        String subfolder1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Subfolder 1", folderToMove);
        String subfolder2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Subfolder 2", folderToMove);
        String deepSubfolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Deep Subfolder", subfolder1);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(folderToMove, newParent, TEST_USER_1_ID);
        fixtures.verifyFolderParent(subfolder1, folderToMove, TEST_USER_1_ID);
        fixtures.verifyFolderParent(subfolder2, folderToMove, TEST_USER_1_ID);
        fixtures.verifyFolderParent(deepSubfolder, subfolder1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder preserves all pages inside")
    public void movingFolderPreservesPages() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);

        String page1 = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", folderToMove);
        String page2 = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", folderToMove);
        String page3 = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 3", folderToMove);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(page1, folderToMove, TEST_USER_1_ID);
        fixtures.verifyPageIsInFolder(page2, folderToMove, TEST_USER_1_ID);
        fixtures.verifyPageIsInFolder(page3, folderToMove, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder preserves pages in nested subfolders")
    public void movingFolderPreservesPagesInNestedFolders() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);
        String subfolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Subfolder", folderToMove);

        String pageInParent = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Parent", folderToMove);
        String pageInChild = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Child", subfolder);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageIsInFolder(pageInParent, folderToMove, TEST_USER_1_ID);
        fixtures.verifyPageIsInFolder(pageInChild, subfolder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder updates child count in old parent")
    public void movingFolderUpdatesOldParentChildCount() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folder1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 1", oldParent);
        String folder2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 2", oldParent);

        fixtures.verifyFolderChildCount(oldParent, 2, TEST_USER_1_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folder1 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderChildCount(oldParent, 1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder updates child count in new parent")
    public void movingFolderUpdatesNewParentChildCount() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);

        fixtures.verifyFolderChildCount(newParent, 0, TEST_USER_1_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderChildCount(newParent, 1, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Moving folder does not affect folder name")
    public void movingFolderPreservesName() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderName = "Important Folder Name";
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, folderName, oldParent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(folderName));
    }

    @Test
    @DisplayName("EDGE: Can move multiple folders to same parent")
    public void canMoveMultipleFoldersToSameParent() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folder1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 1", oldParent);
        String folder2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 2", oldParent);
        String folder3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 3", oldParent);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folder1 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/docs/folders/" + folder2 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/api/docs/folders/" + folder3 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderChildCount(oldParent, 0, TEST_USER_1_ID);
        fixtures.verifyFolderChildCount(newParent, 3, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Can move folder to deeply nested parent")
    public void canMoveFolderToDeeplyNestedParent() throws Exception {
        String rootFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(level4);

        mockMvc.perform(patch("/api/docs/folders/" + rootFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.parentFolderId").value(level4));
    }

    @Test
    @DisplayName("EDGE: Moving folder with shared pages preserves member access")
    public void movingFolderPreservesPageMemberAccess() throws Exception {
        String oldParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Old Parent");
        String newParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "New Parent");
        String folderToMove = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder", oldParent);
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Shared Page", folderToMove);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(newParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderToMove + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot move folder with empty user ID header")
    public void cannotMoveWithEmptyUserId() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String targetParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(targetParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, "")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Cannot move folder with whitespace-only user ID")
    public void cannotMoveWithWhitespaceUserId() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String targetParent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Target");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(targetParent);

        mockMvc.perform(patch("/api/docs/folders/" + folderId + "/move")
                        .header(USER_ID_HEADER, "   ")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Moving folder chain maintains consistency")
    public void movingFolderChainMaintainsConsistency() throws Exception {
        String parent1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 1");
        String parent2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 2");
        String parent3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent 3");

        String travelingFolder = fixtures.createFolderAsUser(TEST_USER_1_ID, "Traveling Folder", parent1);

        fixtures.verifyFolderParent(travelingFolder, parent1, TEST_USER_1_ID);

        String moveToParent2 = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent2);

        mockMvc.perform(patch("/api/docs/folders/" + travelingFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToParent2))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(travelingFolder, parent2, TEST_USER_1_ID);

        String moveToParent3 = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent3);

        mockMvc.perform(patch("/api/docs/folders/" + travelingFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToParent3))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(travelingFolder, parent3, TEST_USER_1_ID);

        String moveToRoot = """
            {
                "parentFolderId": null
            }
            """;

        mockMvc.perform(patch("/api/docs/folders/" + travelingFolder + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveToRoot))
                .andExpect(status().isOk());

        fixtures.verifyFolderIsRoot(travelingFolder, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Sibling folders with same name can exist after move")
    public void siblingFoldersWithSameNameAllowedAfterMove() throws Exception {
        String parent = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String folder1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Name", parent);
        String folder2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Duplicate Name");

        String moveRequest = """
            {
                "parentFolderId": "%s"
            }
            """.formatted(parent);

        mockMvc.perform(patch("/api/docs/folders/" + folder2 + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(moveRequest))
                .andExpect(status().isOk());

        fixtures.verifyFolderParent(folder1, parent, TEST_USER_1_ID);
        fixtures.verifyFolderParent(folder2, parent, TEST_USER_1_ID);
    }
}