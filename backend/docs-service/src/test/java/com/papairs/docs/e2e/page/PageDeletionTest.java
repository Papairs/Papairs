package com.papairs.docs.e2e.page;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Page Deletion")
public class PageDeletionTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to delete their own page")
    public void ownerCanDeleteOwnPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page to Delete");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent page")
    public void deletingNonExistentPageReturns404() throws Exception {
        String nonExistentPageId = "non-existent-page-id";

        mockMvc.perform(delete("/api/docs/pages/" + nonExistentPageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when deleting already deleted page")
    public void deletingAlreadyDeletedPageReturns404() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Delete Twice");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should deny deletion by non-member")
    public void nonMemberCannotDeletePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Private Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should deny deletion by VIEWER member")
    public void viewerCannotDeletePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should allow deletion by EDITOR member")
    public void editorCanDeletePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared with Editor");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Any Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should remove all page members when page is deleted")
    public void deletingPageRemovesAllMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page with Members");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_1_ID);
        fixtures.verifyPageNotInUserList(TEST_USER_2_ID, pageId);
        fixtures.verifyPageNotInUserList(TEST_USER_3_ID, pageId);
    }

    @Test
    @DisplayName("Should remove page from owner's page list after deletion")
    public void deletedPageNotInOwnerList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "To Be Removed");

        fixtures.verifyPageInUserList(TEST_USER_1_ID, pageId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageNotInUserList(TEST_USER_1_ID, pageId);
    }

    @Test
    @DisplayName("Should remove page from member's accessible pages after deletion")
    public void deletedPageNotInMemberList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Then Deleted");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyPageInUserList(TEST_USER_2_ID, pageId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageNotInUserList(TEST_USER_2_ID, pageId);
    }

    @Test
    @DisplayName("EDGE: Deleting page in folder does not delete the folder")
    public void deletingPageDoesNotDeleteFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder with Page");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Folder", folderId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderExists(folderId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting last page in folder leaves folder empty")
    public void deletingLastPageLeavesEmptyFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Only Page", folderId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/folders/" + folderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageCount").value(0));
    }

    @Test
    @DisplayName("EDGE: Deleting one page does not affect other pages")
    public void deletingOnePageDoesNotAffectOthers() throws Exception {
        String page1Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");
        String page3Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 3");

        mockMvc.perform(delete("/api/docs/pages/" + page2Id)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageExists(page1Id, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page2Id, TEST_USER_1_ID);
        fixtures.verifyPageExists(page3Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting page in folder does not affect other pages in same folder")
    public void deletingOnePageInFolderDoesNotAffectSiblings() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String page1Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", folderId);
        String page2Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", folderId);
        String page3Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 3", folderId);

        mockMvc.perform(delete("/api/docs/pages/" + page2Id)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/folders/" + folderId + "/tree")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageCount").value(2));

        fixtures.verifyPageExists(page1Id, TEST_USER_1_ID);
        fixtures.verifyPageExists(page3Id, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting shared page revokes access for all members immediately")
    public void deletedSharedPageRevokesAllAccess() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Widely Shared");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_1_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_2_ID);
        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("EDGE: Can create new page after deleting page with same title")
    public void canCreatePageAfterDeletingSameTitle() throws Exception {
        String title = "Duplicate Title";
        String pageId1 = fixtures.createPageAsUser(TEST_USER_1_ID, title);

        mockMvc.perform(delete("/api/docs/pages/" + pageId1)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        String pageId2 = fixtures.createPageAsUser(TEST_USER_1_ID, title);

        fixtures.verifyPageExists(pageId2, TEST_USER_1_ID);
        assert !pageId1.equals(pageId2) : "New page should have different ID";
    }

    @Test
    @DisplayName("EDGE: Deleting page with content succeeds")
    public void canDeletePageWithContent() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page with Content");

        fixtures.updatePageContent(pageId, TEST_USER_1_ID, "This is some content that will be deleted");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot delete page after leaving as member")
    public void cannotDeleteAfterLeavingAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/leave")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Root-level page can be deleted")
    public void canDeleteRootLevelPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Root Page");

        fixtures.verifyPageHasNoFolder(pageId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageDoesNotExist(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot delete page with empty user ID header")
    public void cannotDeleteWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Some Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot delete page with whitespace-only user ID")
    public void cannotDeleteWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Some Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Deleting parent folder recursively also deletes pages")
    public void recursiveFolderDeletionDeletesPages() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String page1Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 1", folderId);
        String page2Id = fixtures.createPageInFolder(TEST_USER_1_ID, "Page 2", folderId);

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyFolderDoesNotExist(folderId, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page1Id, TEST_USER_1_ID);
        fixtures.verifyPageDoesNotExist(page2Id, TEST_USER_1_ID);
    }
}
