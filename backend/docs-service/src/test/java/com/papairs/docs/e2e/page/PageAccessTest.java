package com.papairs.docs.e2e.page;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Page Access")
public class PageAccessTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to access their own page")
    public void ownerCanAccessOwnPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.title").value("User 1's Page"))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should allow owner to access multiple of their own pages")
    public void ownerCanAccessOwnTwoPage() throws Exception {
        String pageId1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Course 1 Page");
        String pageId2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Course 2 Page");

        mockMvc.perform(get("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.[1].pageId").value(pageId1))
                .andExpect(jsonPath("$.[1].title").value("Course 1 Page"))
                .andExpect(jsonPath("$.[1].ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.[0].pageId").value(pageId2))
                .andExpect(jsonPath("$.[0].title").value("Course 2 Page"))
                .andExpect(jsonPath("$.[0].ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should allow EDITOR member to access shared page")
    public void editorMemberCanAccessPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared with Editor");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should allow VIEWER member to access shared page")
    public void viewerMemberCanAccessPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared with Viewer");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                       .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should deny access to non-member")
    public void nonMemberCannotAccessPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Private Page");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny access after member is removed")
    public void removedMemberLosesAccess() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));

        fixtures.removeMemberFromPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should return 404 for non-existent page")
    public void nonExistentPageReturns404() throws Exception {
        String nonExistentPageId = "non-existent-page-id";

        mockMvc.perform(get("/api/docs/pages/" + nonExistentPageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Any Page");

        mockMvc.perform(get("/api/docs/pages/" + pageId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should allow access after being added as member")
    public void grantAccessWhenAddedAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Initially Private");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should allow multiple different users to access same shared page")
    public void multipleUsersCanAccessSharedPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Public Collaboration");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_3_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Should allow owner to access page even after adding members")
    public void ownerRetainsAccessAfterAddingMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Owner Always Has Access");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("EDGE: Access control is independent of folder ownership")
    public void pageAccessIndependentOfFolderOwnership() throws Exception {
        String user1FolderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "User 1's Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in User 1's Folder", user1FolderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(user1FolderId));

        mockMvc.perform(get("/api/docs/folders/" + user1FolderId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDGE: Third party cannot access page even if they own the folder")
    public void folderOwnerCannotAccessUnsharedPage() throws Exception {
        String user2Id = fixtures.createFolderAsUser(TEST_USER_2_ID, "User 2's Folder");

        String user1Id = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");

        String requestBody = """
            {
                "folderId": "%s"
            }
            """.formatted(user2Id);

        mockMvc.perform(patch("/api/docs/pages/" + user1Id + "/move")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("access")));

        mockMvc.perform(get("/api/docs/pages/" + user1Id)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }
     @Test
     @DisplayName("EDGE: User can access page after role change from EDITOR to VIEWER")
     public void accessMaintainedAfterRoleDowngrade() throws Exception {
     String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Role Change Test");

     fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

     mockMvc.perform(get("/api/docs/pages/" + pageId)
                  .header(USER_ID_HEADER, TEST_USER_2_ID))
             .andExpect(status().isOk());

     fixtures.updateMemberRole(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

     mockMvc.perform(get("/api/docs/pages/" + pageId)
                    .header(USER_ID_HEADER, TEST_USER_2_ID))
             .andExpect(status().isOk());
     }

    @Test
    @DisplayName("EDGE: User can access page after role change from VIEWER to EDITOR")
    public void accessMaintainedAfterRoleUpgrade() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Role Upgrade Test");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());

        fixtures.updateMemberRole(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("EDGE: Cannot access deleted page even as owner")
    public void cannotAccessDeletedPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "To Be Deleted");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EDGE: Member loses access when page is deleted")
    public void memberLosesAccessWhenPageDeleted() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Then Deleted");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/docs/pages/" + pageId)
                .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EDGE: Access to pages in folders deleted by owner")
    public void pageAccessLostWhenFolderDeletedRecursively() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder To Delete");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Folder", folderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/docs/folders/" + folderId)
                        .param("recursive", "true")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EDGE: Cannot access page with empty string user ID")
    public void cannotAccessPageWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Some Page");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("EDGE: Cannot access page with whitespace-only user ID")
    public void cannotAccessPageWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Some Page");

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }
}
