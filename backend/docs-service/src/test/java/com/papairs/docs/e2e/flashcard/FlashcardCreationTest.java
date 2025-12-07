package com.papairs.docs.e2e.flashcard;

import com.papairs.docs.e2e.AbstractE2ETest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@DisplayName("Flashcard Creation")
public class FlashcardCreationTest extends AbstractE2ETest {

    @Test
    @DisplayName("Should create flashcard for a page")
    public void createFlashcardForPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Study Notes");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "What is Java?",
                "answer": "A programming language"
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.flashcardId").exists())
                .andExpect(jsonPath("$.data.pageId").value(pageId))
                .andExpect(jsonPath("$.data.ownerId").value(TEST_USER_1_ID))
                .andExpect(jsonPath("$.data.question").value("What is Java?"))
                .andExpect(jsonPath("$.data.answer").value("A programming language"))
                .andExpect(jsonPath("$.data.learned").value(false))
                .andExpect(jsonPath("$.data.timesReviewed").value(0))
                .andExpect(jsonPath("$.data.timesCorrect").value(0));
    }

    @Test
    @DisplayName("Should create flashcard with tags")
    public void createFlashcardWithTags() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Programming Concepts");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "What is a variable?",
                "answer": "A storage location with a name",
                "tags": ["programming", "basics", "java"]
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.flashcardId").exists())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.tags.length()").value(3))
                .andExpect(jsonPath("$.data.tags[0]").value("programming"))
                .andExpect(jsonPath("$.data.tags[1]").value("basics"))
                .andExpect(jsonPath("$.data.tags[2]").value("java"));
    }

    @Test
    @DisplayName("Should create flashcard with empty tags list")
    public void createFlashcardWithEmptyTags() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Notes");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "Test question",
                "answer": "Test answer",
                "tags": []
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tags").isArray())
                .andExpect(jsonPath("$.data.tags.length()").value(0));
    }

    @Test
    @DisplayName("Should create multiple flashcards for same page")
    public void createMultipleFlashcardsForPage() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Course Material");

        String flashcard1 = """
            {
                "pageId": "%s",
                "question": "Question 1",
                "answer": "Answer 1"
            }
            """.formatted(pageId);

        String flashcard2 = """
            {
                "pageId": "%s",
                "question": "Question 2",
                "answer": "Answer 2"
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(flashcard1))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(flashcard2))
                .andExpect(status().isCreated());

        fixtures.verifyPageHasFlashcardCount(pageId, TEST_USER_1_ID, 2);
    }

    @Test
    @DisplayName("Should create flashcard with whitespace in question and answer")
    public void createFlashcardWithWhitespace() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Notes");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "  Question with spaces  ",
                "answer": "  Answer with spaces  "
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.flashcardId").exists())
                .andExpect(jsonPath("$.data.question").exists())
                .andExpect(jsonPath("$.data.answer").exists());
    }

    @Test
    @DisplayName("Should return 400 when X-User-Id header is missing")
    public void missingUserHeaderReturns400() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Notes");

        String requestBody = """
            {
                "pageId": "%s",
                "question": "Test",
                "answer": "Test"
            }
            """.formatted(pageId);

        mockMvc.perform(post("/api/docs/flashcards")
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("User ID is required")));
    }

    @Test
    @DisplayName("Should handle long question text")
    public void createFlashcardWithLongQuestion() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Detailed Notes");
        String longQuestion = "A".repeat(400);

        String requestBody = """
            {
                "pageId": "%s",
                "question": "%s",
                "answer": "Answer"
            }
            """.formatted(pageId, longQuestion);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.question").value(longQuestion));
    }

    @Test
    @DisplayName("Should handle long answer text")
    public void createFlashcardWithLongAnswer() throws Exception {
        String pageId = fixtures.createPageAsUser(TEST_USER_1_ID, "Detailed Notes");
        String longAnswer = "B".repeat(900);

        String requestBody = """
            {
                "pageId": "%s",
                "question": "Question",
                "answer": "%s"
            }
            """.formatted(pageId, longAnswer);

        mockMvc.perform(post("/api/docs/flashcards")
                        .header(USER_ID_HEADER, TEST_USER_1_ID)
                        .contentType(CONTENT_TYPE_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.answer").value(longAnswer));
    }
}

