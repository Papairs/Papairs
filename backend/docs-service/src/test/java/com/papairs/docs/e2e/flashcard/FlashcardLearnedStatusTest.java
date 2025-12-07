package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Learned Status")
public class FlashcardLearnedStatusTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should update flashcard learned status to true")
    public void updateLearnedStatusToTrue() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.flashcardId").value(flashcardId))
                .andExpect(jsonPath("$.data.learned").value(true))
                .andExpect(jsonPath("$.message").value(containsString("updated")));

        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
    }

    @Test
    @DisplayName("Should update flashcard learned status to false")
    public void updateLearnedStatusToFalse() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content("false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.learned").value(false));

        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);
    }

    @Test
    @DisplayName("Should toggle learned status multiple times")
    public void toggleLearnedStatusMultipleTimes() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
    }

    @Test
    @DisplayName("Should deny update by non-owner")
    public void nonOwnerCannotUpdateLearnedStatus() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1 Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .header(USER_ID_HEADER, TEST_USER_2_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content("true"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);
    }

    @Test
    @DisplayName("Should return 404 for non-existent flashcard")
    public void updateNonExistentFlashcardReturns404() throws Exception {
        String nonExistentFlashcardId = "non-existent-flashcard-id";

        mockMvc.perform(put("/api/docs/flashcards/" + nonExistentFlashcardId + "/learned")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content("true"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .contentType(CONTENT_TYPE_JSON)
                        .content("true"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should update learned status for multiple flashcards independently")
    public void updateMultipleFlashcardsIndependently() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");
        String flashcard3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q3", "A3");

        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);
    }

    @Test
    @DisplayName("Should preserve learned status across retrievals")
    public void preserveLearnedStatusAcrossRetrievals() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].learned").value(true));

        mockMvc.perform(get("/api/docs/flashcards/page/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].learned").value(true));
    }

    @Test
    @DisplayName("Should maintain other flashcard fields when updating learned status")
    public void maintainOtherFieldsWhenUpdatingLearned() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId, "What is Java?", "A programming language", "programming", "java");

        mockMvc.perform(put("/api/docs/flashcards/" + flashcardId + "/learned")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content("true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.question").value("What is Java?"))
                .andExpect(jsonPath("$.data.answer").value("A programming language"))
                .andExpect(jsonPath("$.data.tags.length()").value(2))
                .andExpect(jsonPath("$.data.pageId").value(pageId))
                .andExpect(jsonPath("$.data.ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.data.learned").value(true));
    }
}

