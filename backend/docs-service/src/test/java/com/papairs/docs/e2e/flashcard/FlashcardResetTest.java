package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Reset")
public class FlashcardResetTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should reset all flashcards to unlearned")
    public void resetAllFlashcards() throws Exception {
        String page1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");

        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Q2", "A2");
        String flashcard3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page2, "Q3", "A3");

        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("3 flashcards reset")))
                .andExpect(jsonPath("$.data").value(3));

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, false);
    }

    @Test
    @DisplayName("Should reset only user's own flashcards")
    public void resetOnlyOwnFlashcards() throws Exception {
        String user1Page = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1 Page");
        String user2Page = fixtures.createPageAsUser(TEST_USER_2_ID, "User 2 Page");

        String user1Flashcard = fixtures.createFlashcardAsUser(TEST_USER_1_ID, user1Page, "Q1", "A1");
        String user2Flashcard = fixtures.createFlashcardAsUser(TEST_USER_2_ID, user2Page, "Q2", "A2");

        fixtures.updateFlashcardLearnedStatus(user1Flashcard, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(user2Flashcard, TEST_USER_2_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));

        fixtures.verifyFlashcardLearnedStatus(user1Flashcard, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(user2Flashcard, TEST_USER_2_ID, true);
    }

    @Test
    @DisplayName("Should return zero when user has no flashcards to reset")
    public void resetWithNoFlashcards() throws Exception {
        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("0 flashcards reset")))
                .andExpect(jsonPath("$.data").value(0));
    }

    @Test
    @DisplayName("Should reset all flashcards even if already unlearned")
    public void resetAlreadyUnlearnedFlashcards() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }

    @Test
    @DisplayName("Should reset mixed learned and unlearned flashcards")
    public void resetMixedFlashcards() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");
        String flashcard3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q3", "A3");

        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, false);
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        mockMvc.perform(put("/api/docs/flashcards/reset"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should reset flashcards from multiple pages")
    public void resetFlashcardsFromMultiplePages() throws Exception {
        String page1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Math");
        String page2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Science");
        String page3 = fixtures.createPageAsUser(TEST_USER_1_ID, "History");

        String fc1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Math Q", "Math A");
        String fc2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page2, "Science Q", "Science A");
        String fc3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, page3, "History Q", "History A");

        fixtures.updateFlashcardLearnedStatus(fc1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(fc2, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(fc3, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(3));

        fixtures.verifyFlashcardLearnedStatus(fc1, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(fc2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(fc3, TEST_USER_1_ID, false);
    }

    @Test
    @DisplayName("Should maintain flashcard properties after reset")
    public void maintainPropertiesAfterReset() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId,
            "What is Spring?", "A Java framework", "java", "framework");

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].question").value("What is Spring?"))
                .andExpect(jsonPath("$.data[0].answer").value("A Java framework"))
                .andExpect(jsonPath("$.data[0].tags.length()").value(2))
                .andExpect(jsonPath("$.data[0].learned").value(false));
    }

    @Test
    @DisplayName("Should allow marking flashcards as learned after reset")
    public void allowMarkingLearnedAfterReset() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, false);

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
    }

    @Test
    @DisplayName("Should reset large number of flashcards")
    public void resetLargeNumberOfFlashcards() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Large Study Set");

        for (int i = 1; i <= 10; i++) {
            String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId,
                "Question " + i, "Answer " + i);
            fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        }

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 10);

        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(10));

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[*].learned").value(org.hamcrest.Matchers.everyItem(org.hamcrest.Matchers.is(false))));
    }
}

