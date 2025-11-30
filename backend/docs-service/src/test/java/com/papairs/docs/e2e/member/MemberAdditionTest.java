package com.papairs.docs.e2e.member;

import com.papairs.docs.e2e.AbstractE2ETest;
import com.papairs.docs.model.enums.MemberRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Member Addition")
public class MemberAdditionTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to add member with EDITOR role")
    public void ownerCanAddEditorMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "EDITOR"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.userId").value(TEST_USER_2_ID))
                .andExpect(jsonPath("$.role").value("EDITOR"));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should allow owner to add member with VIEWER role")
    public void ownerCanAddViewerMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.pageId").value(pageId))
                .andExpect(jsonPath("$.userId").value(TEST_USER_2_ID))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("Should deny adding owner as member")
    public void cannotAddOwnerAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "My Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "EDITOR"
            }
            """.formatted(TEST_USER_1_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("owner")));
    }

    @Test
    @DisplayName("Should deny adding same user twice")
    public void cannotAddSameUserTwice() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "EDITOR"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("already")));
    }

    @Test
    @DisplayName("Should deny adding member to non-existent page")
    public void cannotAddMemberToNonExistentPage() throws Exception {
        String nonExistentPageId = "non-existent-page-id";

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + nonExistentPageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should deny non-member from adding members")
    public void nonMemberCannotAddMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1's Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_3_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should deny VIEWER from adding members")
    public void viewerCannotAddMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_3_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Should allow EDITOR from adding members")
    public void editorCannotAddMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_3_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("X-User-Id")));
    }

    @Test
    @DisplayName("Should reject invalid role value")
    public void rejectInvalidRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "INVALID_ROLE"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject null role")
    public void rejectNullRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": null
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject missing userId field")
    public void rejectMissingUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should reject missing role field")
    public void rejectMissingRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Should deny non-existent user from being added as member")
    public void cannotAddNonExistentUserAsMember() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");
        String nonExistentUserId = "non-existent-user-id";

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(nonExistentUserId);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("does not exist")));
    }

    @Test
    @DisplayName("EDGE: Added member immediately gains access to page")
    public void addedMemberGainsImmediateAccess() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        fixtures.verifyUserCannotAccessPage(pageId, TEST_USER_2_ID);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Added member appears in page member list")
    public void addedMemberAppearsInMemberList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.userId == '%s')]".formatted(TEST_USER_2_ID)).exists())
                .andExpect(jsonPath("$[?(@.userId == '%s')].role".formatted(TEST_USER_2_ID)).value("EDITOR"));
    }

    @Test
    @DisplayName("EDGE: Added member sees page in their accessible pages list")
    public void addedMemberSeesPageInList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Shared Page");

        mockMvc.perform(get("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pageId == '%s')]".formatted(pageId)).doesNotExist());

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.pageId == '%s')]".formatted(pageId)).exists());
    }

    @Test
    @DisplayName("EDGE: Can add multiple different members to same page")
    public void canAddMultipleMembersToPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Collaborative Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[?(@.userId == '%s')]".formatted(TEST_USER_2_ID)).exists())
                .andExpect(jsonPath("$[?(@.userId == '%s')]".formatted(TEST_USER_3_ID)).exists());

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_3_ID);
    }

    @Test
    @DisplayName("EDGE: Can add same user to different pages")
    public void canAddSameUserToDifferentPages() throws Exception {
        String page1Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2Id = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");

        fixtures.addMemberToPage(page1Id, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);
        fixtures.addMemberToPage(page2Id, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(page1Id, TEST_USER_2_ID);
        fixtures.verifyUserCanAccessPage(page2Id, TEST_USER_2_ID);

        fixtures.verifyUserCanEditPage(page1Id, TEST_USER_2_ID);
        fixtures.verifyUserCannotEditPage(page2Id, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Adding member to page in folder works correctly")
    public void canAddMemberToPageInFolder() throws Exception {
        String folderId = fixtures.createFolderAsUser(TEST_USER_1_ID, "Folder");
        String pageId = fixtures.createPageInFolder(TEST_USER_1_ID, "Page", folderId);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);

        mockMvc.perform(get("/api/docs/folders/" + folderId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("EDGE: Adding EDITOR grants edit permissions immediately")
    public void addingEditorGrantsEditPermissions() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Adding VIEWER does not grant edit permissions")
    public void addingViewerDoesNotGrantEditPermissions() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        fixtures.verifyUserCanAccessPage(pageId, TEST_USER_2_ID);
        fixtures.verifyUserCannotEditPage(pageId, TEST_USER_2_ID);
    }

    @Test
    @DisplayName("EDGE: Case-insensitive role values accepted")
    public void caseInsensitiveRoleAccepted() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "viewer"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    @DisplayName("EDGE: Cannot add member with empty userId")
    public void cannotAddMemberWithEmptyUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "",
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE: Cannot add member with whitespace-only userId")
    public void cannotAddMemberWithWhitespaceUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "   ",
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE: Cannot add member with null userId")
    public void cannotAddMemberWithNullUserId() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": null,
                "role": "VIEWER"
            }
            """;

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE: Cannot add member with empty role")
    public void cannotAddMemberWithEmptyRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": ""
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE: Cannot add member with whitespace-only role")
    public void cannotAddMemberWithWhitespaceRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "   "
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("EDGE: Adding member does not affect page owner")
    public void addingMemberDoesNotAffectOwner() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId").value(TEST_USER_1_ID));

        fixtures.verifyUserCanEditPage(pageId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("EDGE: Adding member does not affect page content")
    public void addingMemberDoesNotAffectContent() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String content = "Important content";
        fixtures.updatePageContent(pageId, TEST_USER_1_ID, content);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.VIEWER);

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(content));
    }

    @Test
    @DisplayName("EDGE: Adding member to page with existing members succeeds")
    public void canAddMemberToPageWithExistingMembers() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Collaborative Page");

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_2_ID, MemberRole.EDITOR);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 1);

        fixtures.addMemberToPage(pageId, TEST_USER_1_ID, TEST_USER_3_ID, MemberRole.VIEWER);

        fixtures.verifyPageHasMembers(pageId, TEST_USER_1_ID, 2);
    }

    @Test
    @DisplayName("EDGE: Cannot add member with empty X-User-Id header")
    public void cannotAddMemberWithEmptyUserHeader() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, "")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("X-User-Id")));
    }

    @Test
    @DisplayName("EDGE: Cannot add member with whitespace-only X-User-Id header")
    public void cannotAddMemberWithWhitespaceUserHeader() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "VIEWER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, "   ")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("X-User-Id")));
    }

    @Test
    @DisplayName("EDGE: Role value OWNER is rejected")
    public void cannotAddMemberWithOwnerRole() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");

        String addMemberRequest = """
            {
                "userId": "%s",
                "role": "OWNER"
            }
            """.formatted(TEST_USER_2_ID);

        mockMvc.perform(post("/api/docs/pages/" + pageId + "/members")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(addMemberRequest))
                .andExpect(status().isBadRequest());
    }
}