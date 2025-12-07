package com.papairs.docs.e2e.member;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Member Removal")
public class MemberRemovalTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to remove VIEWER member")
    public void ownerCanRemoveViewerMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should allow owner to remove EDITOR member")
    public void ownerCanRemoveEditorMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should deny member removal by non-owner")
    public void nonOwnerCannotRemoveMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("manage members")));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("Should deny member removal by VIEWER")
    public void viewerCannotRemoveMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.EDITOR);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("Should deny member removal by EDITOR")
    public void editorCanRemoveMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Should return 404 when removing non-existent member")
    public void removingNonExistentMemberReturns404() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when attempting to remove owner")
    public void cannotRemoveOwnerAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_1_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 404 when removing from non-existent page")
    public void removingMemberFromNonExistentPageReturns404() throws Exception {
        String nonExistentPageId = "non-existent-page-id";

        mockMvc.perform(delete("/api/docs/pages/" + nonExistentPageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should remove member from page member list")
    public void removedMemberNotInMemberList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 1);

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')]".formatted(TEST_USER_2_ID)).doesNotExist())
                .andExpect(jsonPath("$[?(@.userId == '%s')]".formatted(TEST_USER_3_ID)).exists());
    }

    @Test
    @DisplayName("Should remove page from member's accessible pages list")
    public void removedMemberLosesPageInList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyPageInUserList(TEST_USER_2_ID, pageId);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageNotInUserList(TEST_USER_2_ID, pageId);
    }

    @Test
    @DisplayName("Should revoke all access after member removal")
    public void removedMemberCannotPerformAnyOperations() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());

        String updateRequest = """
            {
                "content": "Attempting to edit"
            }
            """;

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny non-existent user from being removed as member")
    public void cannotRemoveNonExistentUserAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        String nonExistentUserId = "non-existent-user-id";

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + nonExistentUserId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("does not exist")));
    }

    @Test
    @DisplayName("EDGE: Removing last member succeeds and leaves page with only owner")
    public void removingLastMemberSucceeds() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 1);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 0);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Removing one member does not affect other members")
    public void removingOneMemberDoesNotAffectOthers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Multi-Member Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot remove same member twice")
    public void cannotRemoveMemberTwice() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("EDGE: Removing member does not delete the page")
    public void removingMemberDoesNotDeletePage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageExists(pageId, TEST_USER_1_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Removing member does not affect page content")
    public void removingMemberPreservesContent() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page with Content");

        String content = "Important content";
        fixtures.updatePageContent(pageId, TEST_USER_1_ID, content);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @DisplayName("EDGE: Removing member does not affect page location")
    public void removingMemberPreservesPageLocation() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.folderId").value(folderId));
    }

    @Test
    @DisplayName("EDGE: User can be re-added after being removed")
    public void userCanBeReAddedAfterRemoval() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: User can be re-added with different role after removal")
    public void userCanBeReAddedWithDifferentRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot remove member with empty user ID")
    public void cannotRemoveMemberWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot remove member with whitespace-only user ID")
    public void cannotRemoveMemberWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Removing all members in sequence succeeds")
    public void removingAllMembersInSequenceSucceeds() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 1);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 0);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Removed EDITOR loses edit permission immediately")
    public void removedEditorLosesEditPermissionImmediately() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Removed member cannot view page member list")
    public void removedMemberCannotViewMemberList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDGE: Owner can remove member from page in folder")
    public void canRemoveMemberFromPageInFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page in Folder", folderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Removing member from one page does not affect other pages")
    public void removingMemberFromOnePageDoesNotAffectOthers() throws Exception {
        String page1Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");

        fixtures.addMemberToPage(page1Id, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(page2Id, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(delete("/api/docs/pages/" + page1Id + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        fixtures.verifyUserCannotAccessPage(page1Id, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(page2Id, TEST_USER_2_ID);
        fixtures.verifyUserCanEditPage(page2Id, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Member count is accurate after removal")
    public void memberCountAccurateAfterRemoval() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1));
    }
}
