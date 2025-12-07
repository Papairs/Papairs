package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Deletion")
public class FlashcardDeletionTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should allow owner to delete their own flashcard")
    public void ownerCanDeleteOwnFlashcard() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        fixtures.verifyFlashcardExists(flashcardId, TEST_USER_1_ID);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(containsString("deleted successfully")));

        fixtures.verifyFlashcardDoesNotExist(flashcardId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should return 404 when deleting non-existent flashcard")
    public void deletingNonExistentFlashcardReturns404() throws Exception {
        String nonExistentFlashcardId = "non-existent-flashcard-id";

        mockMvc.perform(delete("/api/docs/flashcards/" + nonExistentFlashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("not found")));
    }

    @Test
    @DisplayName("Should return 404 when deleting already deleted flashcard")
    public void deletingAlreadyDeletedFlashcardReturns404() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q", "A");

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Should deny deletion by non-owner")
    public void nonOwnerCannotDeleteFlashcard() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1 Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value(containsString("permission")));

        fixtures.verifyFlashcardExists(flashcardId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should remove flashcard from owner's list after deletion")
    public void deletedFlashcardNotInOwnerList() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "To Delete", "Answer");

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 0);
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));

        fixtures.verifyFlashcardExists(flashcardId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should delete flashcard and update page flashcard count")
    public void deletingFlashcardUpdatesPageCount() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");

        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcard1)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 1);
    }

    @Test
    @DisplayName("Should delete all flashcards individually")
    public void deleteAllFlashcardsIndividually() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");
        String flashcard3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q3", "A3");

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 3);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcard1)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 2);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcard2)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcard3)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 0);
    }

    @Test
    @DisplayName("Should delete flashcard with tags")
    public void deleteFlashcardWithTags() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Tagged Page");
        String flashcardId = fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId, "Q", "A", "tag1", "tag2", "tag3");

        fixtures.verifyFlashcardExists(flashcardId, TEST_USER_1_ID);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyFlashcardDoesNotExist(flashcardId, TEST_USER_1_ID);
    }

    @Test
    @DisplayName("Should delete learned flashcard")
    public void deleteLearnedFlashcard() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q", "A");

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardExists(flashcardId, TEST_USER_1_ID);

        mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyFlashcardDoesNotExist(flashcardId, TEST_USER_1_ID);
    }
}

