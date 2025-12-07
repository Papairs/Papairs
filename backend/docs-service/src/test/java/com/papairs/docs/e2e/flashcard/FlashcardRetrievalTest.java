package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Retrieval")
public class FlashcardRetrievalTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should retrieve all flashcards for a user")
    public void getUserFlashcards() throws Exception {
        String page1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");

        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Question 1", "Answer 1");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page2, "Question 2", "Answer 2");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.data[1].ownerId").value(TEST_USER_1_ID));
    }

    @Test
    @DisplayName("Should return empty list when user has no flashcards")
    public void getUserFlashcardsEmpty() throws Exception {
        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Should retrieve flashcards for a specific page")
    public void getPageFlashcards() throws Exception {
        String page1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 1");
        String page2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Page 2");

        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Q1", "A1");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Q2", "A2");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page2, "Q3", "A3");

        mockMvc.perform(get("/api/docs/flashcards/page/" + page1)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].pageId").value(page1))
                .andExpect(jsonPath("$.data[1].pageId").value(page1));
    }

    @Test
    @DisplayName("Should return empty list when page has no flashcards")
    public void getPageFlashcardsEmpty() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Empty Page");

        mockMvc.perform(get("/api/docs/flashcards/page/" + pageId)
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    @DisplayName("Should only return flashcards owned by the requesting user")
    public void getUserFlashcardsIsolation() throws Exception {
        String user1Page = fixtures.createPageAsUser(TEST_USER_1_ID, "User 1 Page");
        String user2Page = fixtures.createPageAsUser(TEST_USER_2_ID, "User 2 Page");

        fixtures.createFlashcardAsUser(TEST_USER_1_ID, user1Page, "User 1 Question", "User 1 Answer");
        fixtures.createFlashcardAsUser(TEST_USER_2_ID, user2Page, "User 2 Question", "User 2 Answer");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerId").value(TEST_USER_1_ID));

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_2_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].ownerId").value(TEST_USER_2_ID));
    }

    @Test
    @DisplayName("Should retrieve flashcards with correct learned status")
    public void getFlashcardsWithLearnedStatus() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        String flashcardId = fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].learned").value(false));

        fixtures.updateFlashcardLearnedStatus(flashcardId, TEST_USER_1_ID, true);

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].learned").value(true));
    }

    @Test
    @DisplayName("Should retrieve flashcards with tags")
    public void getFlashcardsWithTags() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Tagged Page");
        fixtures.createFlashcardWithTags(TEST_USER_1_ID, pageId, "Question", "Answer", "tag1", "tag2");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].tags").isArray())
                .andExpect(jsonPath("$.data[0].tags.length()").value(2))
                .andExpect(jsonPath("$.data[0].tags[0]").value("tag1"))
                .andExpect(jsonPath("$.data[0].tags[1]").value("tag2"));
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        mockMvc.perform(get("/api/docs/flashcards"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should retrieve multiple flashcards from different pages")
    public void getFlashcardsFromMultiplePages() throws Exception {
        String page1 = fixtures.createPageAsUser(TEST_USER_1_ID, "Math");
        String page2 = fixtures.createPageAsUser(TEST_USER_1_ID, "Science");
        String page3 = fixtures.createPageAsUser(TEST_USER_1_ID, "History");

        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page1, "Math Q", "Math A");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page2, "Science Q", "Science A");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, page3, "History Q", "History A");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3));
    }

    @Test
    @DisplayName("Should retrieve correct review statistics")
    public void getFlashcardsWithReviewStats() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Page");
        fixtures.createFlashcardAsUser(TEST_USER_1_ID, pageId, "Question", "Answer");

        mockMvc.perform(get("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].timesReviewed").value(0))
                .andExpect(jsonPath("$.data[0].timesCorrect").value(0))
                .andExpect(jsonPath("$.data[0].lastReviewed").isEmpty())
                .andExpect(jsonPath("$.data[0].nextReviewDate").isEmpty());
    }
}

