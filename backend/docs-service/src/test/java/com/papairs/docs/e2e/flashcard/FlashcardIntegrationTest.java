package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Integration")
public class FlashcardIntegrationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should handle complete flashcard lifecycle")
    public void completeFlashcardLifecycle() throws Exception {
        // Create a page
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Notes");

        // Create flashcards
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId,
            "What is polymorphism?", "The ability of objects to take many forms");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId,
            "What is encapsulation?", "Hiding internal state and requiring interaction through methods");

        // Verify flashcards exist
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 2);
        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 2);

        // Mark one as learned
        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);

        // Reset all
        mockMvc.perform(put("/api/docs/flashcards/reset")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);

        // Mark both as learned
        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, true);

        // Delete one
        mockMvc.perform(delete("/api/docs/flashcards/" + flashcard1)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);
    }

    @Test
    @DisplayName("Should isolate flashcards between different users")
    public void flashcardIsolationBetweenUsers() throws Exception {
        String user1Page = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1 Notes");
        String user2Page = fixtures.createPageAsUser(TEST_USER_2_ID, "User 2 Notes");

        String user1Flashcard = fixtures.createFlashcardAsUser(TEST_USER_1_ID, user1Page, "Q1", "A1");
        String user2Flashcard = fixtures.createFlashcardAsUser(TEST_USER_2_ID, user2Page, "Q2", "A2");

        // Each user should only see their own flashcards
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);
        fixtures.verifyUserHasFlashcardCount(TEST_USER_2_ID, 1);

        // User 1 marks their flashcard as learned
        fixtures.updateFlashcardLearnedStatus(user1Flashcard, TEST_USER_1_ID, true);

        // Should not affect User 2's flashcards
        fixtures.verifyFlashcardLearnedStatus(user2Flashcard, TEST_USER_2_ID, false);

        // User 2 cannot access User 1's flashcard
        mockMvc.perform(delete("/api/docs/flashcards/" + user1Flashcard)
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isForbidden());

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);
    }

    @Test
    @DisplayName("Should handle flashcards with special characters")
    public void flashcardsWithSpecialCharacters() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Special Characters");

        String specialQuestion = "What is \"JSON\"? & how does it work?";
        String specialAnswer = "It's a data format with {key: 'value'} pairs & arrays [1, 2, 3]";

        String requestBody = """
            {
                "pageId": "%s",
                "question": "%s",
                "answer": "%s"
            }
            """.formatted(pageId,
                specialQuestion.replace("\"", "\\\""),
                specialAnswer.replace("\"", "\\\""));

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.question").exists())
                .andExpect(jsonPath("$.data.answer").exists());
    }

    @Test
    @DisplayName("Should handle multiple pages with flashcards")
    public void multiplePagesWithFlashcards() throws Exception {
        String mathPage = fixtures.createPageAsUser(TEST_USER_1_ID, "Mathematics");
        String sciencePage = fixtures.createPageAsUser(TEST_USER_1_ID, "Science");
        String historyPage = fixtures.createPageAsUser(TEST_USER_1_ID, "History");

        // Create 2 flashcards for math
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, mathPage, "What is Pi?", "3.14159...");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, mathPage, "What is E?", "2.71828...");

        // Create 3 flashcards for science
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, sciencePage, "What is H2O?", "Water");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, sciencePage, "What is CO2?", "Carbon Dioxide");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, sciencePage, "What is O2?", "Oxygen");

        // Create 1 flashcard for history
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, historyPage, "When was WWI?", "1914-1918");

        // Verify counts
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 6);
        fixtures.verifyPageHasFlashcardCount(mathPage, TEST_USER_1_ID, 2);
        fixtures.verifyPageHasFlashcardCount(sciencePage, TEST_USER_1_ID, 3);
        fixtures.verifyPageHasFlashcardCount(historyPage, TEST_USER_1_ID, 1);
    }

    @Test
    @DisplayName("Should handle flashcards with various tags")
    public void flashcardsWithVariousTags() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Tagged Content");

        // No tags
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");

        // Single tag
        fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId, "Q2", "A2", "important");

        // Multiple tags
        fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId, "Q3", "A3",
            "java", "programming", "oop", "basics");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("Should maintain flashcard count consistency after operations")
    public void maintainCountConsistency() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Count Test");

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 0);

        // Add 5 flashcards
        String fc1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 1);

        String fc2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 2);

        String fc3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q3", "A3");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 3);

        String fc4 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q4", "A4");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 4);

        String fc5 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q5", "A5");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 5);

        // Delete 2 flashcards
        mockMvc.perform(delete("/api/docs/flashcards/" + fc2)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 4);

        mockMvc.perform(delete("/api/docs/flashcards/" + fc4)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 3);

        // Add 1 more
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q6", "A6");
        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 4);
    }

    @Test
    @DisplayName("Should handle concurrent learned status updates")
    public void concurrentLearnedStatusUpdates() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcard1 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        String flashcard2 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");
        String flashcard3 = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q3", "A3");

        // Update all to learned
        fixtures.updateFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, true);
        fixtures.updateFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        // Verify all are learned
        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);

        // Update one back to unlearned
        fixtures.updateFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);

        // Verify states
        fixtures.verifyFlashcardLearnedStatus(flashcard1, TEST_USER_1_ID, true);
        fixtures.verifyFlashcardLearnedStatus(flashcard2, TEST_USER_1_ID, false);
        fixtures.verifyFlashcardLearnedStatus(flashcard3, TEST_USER_1_ID, true);
    }

    @Test
    @DisplayName("Should preserve flashcards when page is accessed by owner")
    public void preserveFlashcardsOnPageAccess() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Notes");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q1", "A1");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Q2", "A2");

        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 2);

        // Access the page multiple times
        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/docs/pages/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk());

        // Flashcards should still exist
        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 2);
    }

    @Test
    @DisplayName("Should create flashcard with maximum allowed text length")
    public void createFlashcardWithMaxLength() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Long Content");
        String maxQuestion = "Q".repeat(500);
        String maxAnswer = "A".repeat(1000);

        String requestBody = """
            {
                "pageId": "%s",
                "question": "%s",
                "answer": "%s"
            }
            """.formatted(pageId, maxQuestion, maxAnswer);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.question").value(maxQuestion))
                .andExpect(jsonPath("$.data.answer").value(maxAnswer));
    }

    @Test
    @DisplayName("Should handle rapid creation and deletion")
    public void rapidCreationAndDeletion() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Rapid Test");

        for (int i = 0; i < 5; i++) {
            String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId,
                "Question " + i, "Answer " + i);

            mockMvc.perform(delete("/api/docs/flashcards/" + flashcardId)
                            .header(USER_ID_HEADER, TEST_USER_1_ID))
                    .andExpect(status().isOk());
        }

        fixtures.verifyUserHasFlashcardCount(TEST_USER_1_ID, 0);
    }
}

