package com.papairs.docs.e2e.member;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Member Role Update")
public class MemberRoleUpdateTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to upgrade member from VIEWER to EDITOR")
    public void ownerCanUpgradeViewerToEditor() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_2_ID))
                .andExpect(jsonPath("$.role").value(MemberRole.EDITOR.name()));

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should allow owner to downgrade member from EDITOR to VIEWER")
    public void ownerCanDowngradeEditorToViewer() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);

        String updateRequest = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(TEST_USER_2_ID))
                .andExpect(jsonPath("$.role").value(MemberRole.VIEWER.name()));

        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should deny role update by non-member")
    public void nonMemberCannotUpdateRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_3_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("manage members")));

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.VIEWER);
    }

    @Test
    @DisplayName("Should deny role update by VIEWER member")
    public void viewerCannotUpdateRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isForbidden());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_3_ID, MemberRole.VIEWER);
    }

    @Test
    @DisplayName("Should deny role update by EDITOR member")
    public void editorCanUpdateRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_3_ID)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_3_ID, MemberRole.EDITOR);
    }

    @Test
    @DisplayName("Should return 404 when updating non-existent member")
    public void updatingNonExistentMemberReturns404() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when updating member on non-existent page")
    public void updatingMemberOnNonExistentPageReturns404() throws Exception {
        String nonExistentPageId = "non-existent-page-id";

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + nonExistentPageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should return 400 when newRole is invalid")
    public void invalidRoleReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "ADMIN"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when newRole is empty")
    public void emptyRoleReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": ""
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should return 400 when newRole is null")
    public void nullRoleReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": null
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should deny non-existent user from being removed as member")
    public void cannotUpdateNonExistentUserAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        String nonExistentUserId = "non-existent-user-id";

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + nonExistentUserId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("does not exist")));
    }

    @Test
    @DisplayName("EDGE: Updating to same role succeeds idempotently")
    public void updatingToSameRoleSucceeds() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(MemberRole.EDITOR.name()));

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Role update does not affect other members")
    public void roleUpdateDoesNotAffectOtherMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.EDITOR);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.verifyMemberHasRole(pageId, TEST_USER_3_ID, MemberRole.EDITOR);
    }

    @Test
    @DisplayName("EDGE: Role update does not affect page content")
    public void roleUpdateDoesNotAffectPageContent() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page with Content");
        String content = "Important content";
        fixtures.updatePageContent(pageId, TEST_USER_1_ID, content);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @DisplayName("EDGE: Role update does not affect page ownership")
    public void roleUpdateDoesNotAffectOwnership() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("EDGE: Multiple role changes in sequence work correctly")
    public void multipleRoleChangesInSequence() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);

        String upgradeRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(upgradeRequest))
                .andExpect(status().isOk());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);

        String downgradeRequest = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(downgradeRequest))
                .andExpect(status().isOk());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(upgradeRequest))
                .andExpect(status().isOk());

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: VIEWER upgraded to EDITOR can immediately edit")
    public void upgradedMemberCanImmediatelyEdit() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        String contentUpdate = """
            {
                "content": "User 2's immediate edit"
            }
            """;

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(contentUpdate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value("User 2's immediate edit"));
    }

    @Test
    @DisplayName("EDGE: EDITOR downgraded to VIEWER loses edit permission immediately")
    public void downgradedMemberLosesEditPermissionImmediately() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);

        String updateRequest = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        String contentUpdate = """
            {
                "content": "User 2 tries to edit"
            }
            """;

        mockMvc.perform(put("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(contentUpdate))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDGE: Role case should be normalized correctly")
    public void roleCaseIsHandledCorrectly() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "edITor"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value(MemberRole.EDITOR.name()));
    }

    @Test
    @DisplayName("EDGE: Member count remains same after role update")
    public void memberCountRemainsAfterRoleUpdate() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.EDITOR);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);
    }

    @Test
    @DisplayName("EDGE: Cannot update owner role through member endpoint")
    public void cannotUpdateOwnerThroughMemberEndpoint() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String updateRequest = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_1_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("EDGE: Role update preserves member's access to page")
    public void roleUpdatePreservesPageAccess() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        String updateToEditor = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateToEditor))
                .andExpect(status().isOk());

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        String updateToViewer = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateToViewer))
                .andExpect(status().isOk());

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Cannot update role with empty user ID header")
    public void cannotUpdateRoleWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, "")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.VIEWER);
    }

    @Test
    @DisplayName("EDGE: Cannot update role with whitespace-only user ID")
    public void cannotUpdateRoleWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, "   ")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyMemberHasRole(pageId, TEST_USER_2_ID, MemberRole.VIEWER);
    }

    @Test
    @DisplayName("EDGE: Role update appears immediately in member list")
    public void roleUpdateAppearsInMemberList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')].role".formatted(TEST_USER_2_ID)).value(MemberRole.VIEWER.name()));

        String updateRequest = """
            {
                "role": "EDITOR"
            }
            """;

        mockMvc.perform(patch("/api/docs/pages/" + pageId + "/members/" + TEST_USER_2_ID)
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(updateRequest))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')].role".formatted(TEST_USER_2_ID)).value(MemberRole.EDITOR.name()));
    }
}