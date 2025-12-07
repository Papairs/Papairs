package com.papairs.docs.e2e.folder;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Folder Deletion")
public class FolderDeletionTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to delete empty folder")
    public void ownerCanDeleteEmptyFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Empty Folder");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny non-recursive deletion when folder has subfolders")
    public void nonRecursiveDeletionFailsWithSubfolders() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parentFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + parentFolderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("not empty")));

        fixtures.verifyFolderExists(parentFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderExists(childFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny non-recursive deletion when folder has pages")
    public void nonRecursiveDeletionFailsWithPages() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder with Pages");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(containsString("not empty")));

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow recursive deletion of folder with subfolders")
    public void recursiveDeletionRemovesSubfolders() throws Exception {
        String rootFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", rootFolderId);
        String grandchildFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild", childFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + rootFolderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(rootFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(childFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(grandchildFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow recursive deletion of folder with pages")
    public void recursiveDeletionRemovesPages() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String page1Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", folderId);
        String page2Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", folderId);
        String page3Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 3", folderId);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(folderId, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page1Id, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page2Id, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page3Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny deletion by non-owner")
    public void nonOwnerCannotDeleteFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent folder")
    public void deletingNonExistentFolderReturns404() throws Exception {
        String nonExistentFolderId = "non-existent-folder-id";

        mockMvc.perform(delete("/api/docs/folders/" + nonExistentFolderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when deleting already deleted folder")
    public void deletingAlreadyDeletedFolderReturns404() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Delete Twice");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Any Folder");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should remove folder from parent's children list after deletion")
    public void deletedFolderNotInParentChildren() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parentFolderId);

        fixtures.verifyFolderInChildrenList(parentFolderId, childFolderId, TEST_USER_1_ID);

        mockMvc.perform(delete("/api/docs/folders/" + childFolderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderNotInChildrenList(parentFolderId, childFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should remove folder from root folders list after deletion")
    public void deletedRootFolderNotInRootList() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root Folder");

        fixtures.verifyFolderInRootList(TEST_USER_1_ID, folderId);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderNotInRootList(TEST_USER_1_ID, folderId);
    }

    @Test
    @DisplayName("Should remove folder from all folders list after deletion")
    public void deletedFolderNotInAllFoldersList() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "To Remove");

        fixtures.verifyFolderInAllFoldersList(TEST_USER_1_ID, folderId);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderNotInAllFoldersList(TEST_USER_1_ID, folderId);
    }

    @Test
    @DisplayName("EDGE: Recursive deletion removes all pages in tree")
    public void recursiveDeletionRemovesAllPagesInTree() throws Exception {
        String rootFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", rootFolderId);
        String grandchildFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild", childFolderId);

        String pageInRoot = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Root", rootFolderId);
        String pageInChild = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Child", childFolderId);
        String pageInGrandchild = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Grandchild", grandchildFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + rootFolderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageInRoot, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(pageInChild, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(pageInGrandchild, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Recursive deletion removes shared pages and revokes member access")
    public void recursiveDeletionRevokesSharedPageAccess() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Shared Content");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Shared Page", folderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_2_ID);
        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_3_ID);
        fixtures.verifyPageNotInUserList(TEST_USER_2_ID, pageId);
        fixtures.verifyPageNotInUserList(TEST_USER_3_ID, pageId);
    }

    @Test
    @DisplayName("EDGE: Deleting one sibling folder does not affect other siblings")
    public void deletingOneSiblingDoesNotAffectOthers() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", parentFolderId);
        String child2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", parentFolderId);
        String child3Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 3", parentFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + child2Id)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderExists(parentFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderExists(child1Id, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(child2Id, TEST_USER_1_ID);
        fixtures.verifyFolderExists(child3Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting parent folder does not affect unrelated folders")
    public void deletingParentDoesNotAffectUnrelatedFolders() throws Exception {
        String folder1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 1");
        String folder2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder 2");
        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child of 1", folder1Id);

        mockMvc.perform(delete("/api/docs/folders/" + folder1Id)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(folder1Id, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(child1Id, TEST_USER_1_ID);
        fixtures.verifyFolderExists(folder2Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting deeply nested folder structure succeeds")
    public void canDeleteDeeplyNestedStructure() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);
        String level4 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 4", level3);
        String level5 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 5", level4);

        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Level 3", level3);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Level 5", level5);

        mockMvc.perform(delete("/api/docs/folders/" + level1)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(level1, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(level2, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(level3, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(level4, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(level5, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting folder with many subfolders succeeds")
    public void canDeleteFolderWithManyChildren() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");

        for (int i = 1; i <= 10; i++) {
            fixtures.createFolderAsUser(TEST_USER_1_ID, "Child " + i, parentFolderId);
        }

        mockMvc.perform(delete("/api/docs/folders/" + parentFolderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(parentFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting folder with many pages succeeds")
    public void canDeleteFolderWithManyPages() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");

        for (int i = 1; i <= 50; i++) {
            fixtures.createPageInFolder(TEST_USER_1_ID, "Page " + i, folderId);
        }

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Can create new folder after deleting folder with same name")
    public void canCreateFolderAfterDeletingSameName() throws Exception {
        String name = "Duplicate Name";
        String folderId1 = fixtures.createFolderAsUser(TEST_USER_1_ID, name);

        mockMvc.perform(delete("/api/docs/folders/" + folderId1)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        String folderId2 = fixtures.createFolderAsUser(TEST_USER_1_ID, name);

        fixtures.verifyFolderExists(folderId2, TEST_USER_1_ID);
        assert !folderId1.equals(folderId2) : "New folder should have different ID";
    }

    @Test
    @DisplayName("EDGE: Non-recursive deletion default is false")
    public void nonRecursiveDeletionIsDefault() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parentFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + parentFolderId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isConflict());

        fixtures.verifyFolderExists(parentFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderExists(childFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting child folder does not affect parent")
    public void deletingChildDoesNotAffectParent() throws Exception {
        String parentFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Parent");
        String childFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child", parentFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + childFolderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderExists(parentFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(childFolderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot delete folder with empty user ID header")
    public void cannotDeleteWithEmptyUserId() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Some Folder");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot delete folder with whitespace-only user ID")
    public void cannotDeleteWithWhitespaceUserId() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Some Folder");

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Recursive deletion with mixed content succeeds")
    public void recursiveDeletionWithMixedContentSucceeds() throws Exception {
        String rootFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", rootFolderId);
        String child2Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", rootFolderId);
        String grandchildId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Grandchild", child1Id);

        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Root", rootFolderId);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1 in Child 1", child1Id);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2 in Child 1", child1Id);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Child 2", child2Id);
        fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Grandchild", grandchildId);

        mockMvc.perform(delete("/api/docs/folders/" + rootFolderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(rootFolderId, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(child1Id, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(child2Id, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(grandchildId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Empty folder at any nesting level can be deleted non-recursively")
    public void emptyNestedFolderCanBeDeletedNonRecursively() throws Exception {
        String level1 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 1");
        String level2 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 2", level1);
        String level3 = fixtures.createFolderAsUser(TEST_USER_1_ID, "Level 3", level2);

        mockMvc.perform(delete("/api/docs/folders/" + level3)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderExists(level1, TEST_USER_1_ID);
        fixtures.verifyFolderExists(level2, TEST_USER_1_ID);
        fixtures.verifyFolderDoesNotExist(level3, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Folder tree is updated after deletion")
    public void folderTreeUpdatedAfterDeletion() throws Exception {
        String rootFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root");
        String child1Id = fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 1", rootFolderId);
        fixtures.createFolderAsUser(TEST_USER_1_ID, "Child 2", rootFolderId);

        mockMvc.perform(get("/api/docs/folders/" + rootFolderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childCount").value(2));

        mockMvc.perform(delete("/api/docs/folders/" + child1Id)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/folders/" + rootFolderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.childCount").value(1));
    }

    @Test
    @DisplayName("EDGE: Root-level folder can be deleted")
    public void canDeleteRootLevelFolder() throws Exception {
        String rootFolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Root Folder");

        fixtures.verifyFolderHasNoParent(rootFolderId);

        mockMvc.perform(delete("/api/docs/folders/" + rootFolderId)
                        .param("recursive", "false")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(rootFolderId, TEST_USER_1_ID);
    }
}
