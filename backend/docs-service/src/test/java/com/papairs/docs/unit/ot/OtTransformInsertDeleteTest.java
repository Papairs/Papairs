package com.papairs.docs.unit.ot;

import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.unit.fixture.OperationFixtures;
import com.papairs.docs.util.OtTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OT Transform: Insert + Delete")
class OtTransformInsertDeleteTest {

    @Nested
    @DisplayName("When insert is after delete range")
    class InsertAfterDelete {

        @Test
        @DisplayName("Insert after delete → position shifts left by delete length")
        void insertAfterDelete_shiftsLeft() {
            InsertOp insert = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(7, transformed.pos,
                    "Position should shift left by delete length (10 - 3 = 7)");
            assertEquals("Hello", transformed.text,
                    "Text should remain unchanged");
        }

        @Test
        @DisplayName("Insert far after delete → shifts left by full delete length")
        void insertFarAfterDelete_shiftsLeftByFullLength() {
            InsertOp insert = OperationFixtures.createInsert(100, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(95, transformed.pos,
                    "Position should shift by full delete length (100 - 5 = 95)");
        }

        @Test
        @DisplayName("Insert immediately after delete range → shifts to delete end")
        void insertImmediatelyAfterDelete_shiftsToDeleteEnd() {
            InsertOp insert = OperationFixtures.createInsert(8, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2"); // Deletes 5-7

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos,
                    "Insert at position 8 should move to position 5 (end of delete)");
        }

        @ParameterizedTest(name = "Insert at {0}, delete at {1} length {2} → shifts to {3}")
        @CsvSource({
                "10, 5, 3, 7",      // Simple case
                "10, 5, 5, 5",      // Delete exactly reaches insert
                "20, 10, 5, 15",    // Larger numbers
                "50, 0, 10, 40",    // Delete from start
                "100, 50, 20, 80"   // Large positions
        })
        @DisplayName("Various after-delete scenarios shift correctly")
        void variousAfterDeleteScenarios(int insertPos, int deletePos,
                                         int deleteLen, int expectedPos) {
            InsertOp insert = OperationFixtures.createInsert(insertPos, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(deletePos, deleteLen, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(expectedPos, transformed.pos);
        }
    }

    @Nested
    @DisplayName("When insert is before or at delete position")
    class InsertBeforeOrAtDelete {

        @Test
        @DisplayName("Insert before delete → position unchanged")
        void insertBeforeDelete_noChange() {
            InsertOp insert = OperationFixtures.createInsert(3, "Hello", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(3, transformed.pos,
                    "Insert before delete should not change position");
            assertEquals("Hello", transformed.text);
        }

        @Test
        @DisplayName("Insert at delete position → position unchanged")
        void insertAtDeletePosition_noChange() {
            InsertOp insert = OperationFixtures.createInsert(5, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos,
                    "Insert at delete start should not change position");
        }

        @Test
        @DisplayName("Insert at position 0 with delete after → unchanged")
        void insertAtZero_unchanged() {
            InsertOp insert = OperationFixtures.createInsert(0, "Start", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(0, transformed.pos);
        }

        @ParameterizedTest(name = "Insert at {0}, delete at {1} → no change")
        @CsvSource({
                "0, 5",
                "3, 10",
                "5, 5",     // At delete position
                "10, 15",
                "50, 100"
        })
        @DisplayName("Various before/at scenarios remain unchanged")
        void variousBeforeAtScenarios_unchanged(int insertPos, int deletePos) {
            InsertOp insert = OperationFixtures.createInsert(insertPos, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(deletePos, 5, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(insertPos, transformed.pos,
                    "Position should not change");
        }
    }

    @Nested
    @DisplayName("When insert is within delete range")
    class InsertWithinDeleteRange {

        @Test
        @DisplayName("Insert in middle of delete range → shifts to delete position")
        void insertInMiddleOfDeleteRange_shiftsToDeletePosition() {
            InsertOp insert = OperationFixtures.createInsert(7, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 5, "client2", "op2"); // Deletes 5-9

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos,
                    "Insert within delete range should move to delete start position");
        }

        @Test
        @DisplayName("Insert one position after delete start → shifts to delete start")
        void insertJustAfterDeleteStart_shiftsToDeleteStart() {
            InsertOp insert = OperationFixtures.createInsert(6, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 10, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos,
                    "Should shift to delete start (6 - min(10, 1) = 5)");
        }

        @Test
        @DisplayName("Insert near end of delete range → shifts to delete start")
        void insertNearEndOfDeleteRange_shiftsToDeleteStart() {
            InsertOp insert = OperationFixtures.createInsert(14, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2"); // Deletes 10-14

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(10, transformed.pos,
                    "Insert at last position of delete should move to delete start");
        }

        @ParameterizedTest(name = "Insert at {0} within delete({1},{2}) → shifts to {1}")
        @CsvSource({
                "6, 5, 5",      // Insert at pos 6, delete 5-9
                "11, 10, 5",    // Insert at pos 11, delete 10-14
                "8, 5, 10",     // Insert at pos 8, delete 5-14
                "51, 50, 10",   // Larger numbers
        })
        @DisplayName("Various within-range scenarios shift to delete start")
        void variousWithinRangeScenarios(int insertPos, int deletePos, int deleteLen) {
            InsertOp insert = OperationFixtures.createInsert(insertPos, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(deletePos, deleteLen, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(deletePos, transformed.pos,
                    "Insert within range should move to delete start");
        }
    }

    @Nested
    @DisplayName("Delete length variations")
    class DeleteLengthVariations {

        @Test
        @DisplayName("Delete length 0 → no transformation")
        void deleteLength_zero_noEffect() {
            InsertOp insert = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 0, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(10, transformed.pos,
                    "Zero-length delete should not affect position");
        }

        @Test
        @DisplayName("Delete length 1 (single character)")
        void deleteLength_one() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 1, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(9, transformed.pos,
                    "Should shift left by 1");
        }

        @Test
        @DisplayName("Very large delete length")
        void deleteLength_veryLarge() {
            InsertOp insert = OperationFixtures.createInsert(20, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 100, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            // Insert at 20 is within delete range 5-105
            // Should shift to delete start: 20 - min(100, 20-5) = 20 - 15 = 5
            assertEquals(5, transformed.pos,
                    "Large delete should move insert to delete start");
        }

        @ParameterizedTest(name = "Delete length {1} shifts insert from {0}")
        @CsvSource({
                "10, 1, 9",
                "10, 2, 8",
                "10, 5, 5",
                "10, 10, 0",
                "10, 15, 0"   // Delete longer than distance
        })
        @DisplayName("Various delete lengths produce correct shifts")
        void variousDeleteLengths(int insertPos, int deleteLen, int expectedPos) {
            InsertOp insert = OperationFixtures.createInsert(insertPos, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(0, deleteLen, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(expectedPos, transformed.pos);
        }
    }

    @Nested
    @DisplayName("Partial overlap and boundary scenarios")
    class PartialOverlapScenarios {

        @Test
        @DisplayName("Delete covers part of distance to insert → partial shift")
        void deleteCoversPartialDistance() {
            InsertOp insert = OperationFixtures.createInsert(20, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2"); // Deletes 10-14

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            // Distance from delete to insert: 20 - 10 = 10
            // Delete length: 5
            // Shift by min(5, 10) = 5
            assertEquals(15, transformed.pos,
                    "Should shift by min(deleteLen, distance) = min(5, 10) = 5");
        }

        @Test
        @DisplayName("Delete covers exact distance to insert → shifts to delete start")
        void deleteCoversExactDistance() {
            InsertOp insert = OperationFixtures.createInsert(15, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2"); // Deletes 10-14

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(10, transformed.pos,
                    "Delete exactly reaching insert should move it to delete start");
        }

        @Test
        @DisplayName("Delete exceeds distance to insert → shifts to delete start")
        void deleteExceedsDistance() {
            InsertOp insert = OperationFixtures.createInsert(12, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 10, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            // Distance: 12 - 10 = 2
            // Delete length: 10
            // Shift by min(10, 2) = 2
            assertEquals(10, transformed.pos, "Should shift by available distance (insert ends up at delete start)");
        }

        @Test
        @DisplayName("Multiple character delete with insert one position after")
        void multiCharDelete_insertOneAfter() {
            InsertOp insert = OperationFixtures.createInsert(6, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            // Distance: 6 - 5 = 1
            // Delete length: 3
            // Shift by min(3, 1) = 1
            assertEquals(5, transformed.pos);
        }
    }

    @Nested
    @DisplayName("Metadata preservation")
    class MetadataPreservation {

        @Test
        @DisplayName("Text content remains unchanged")
        void text_unchanged() {
            InsertOp insert = OperationFixtures.createInsert(10, "Hello World", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals("Hello World", transformed.text, "Text should never change during transformation");
        }

        @Test
        @DisplayName("clientId and opId remain unchanged")
        void identifiers_preserved() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", "myClient", "myOp123");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "otherClient", "otherOp");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals("myClient", transformed.clientId);
            assertEquals("myOp123", transformed.opId);
        }

        @Test
        @DisplayName("baseVersion remains unchanged")
        void baseVersion_preserved() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", "client1", "op1");
            insert.baseVersion = 42;
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(42, transformed.baseVersion);
        }

        @Test
        @DisplayName("type field remains 'insert'")
        void type_remainsInsert() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals("insert", transformed.type);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Delete at position 0")
        void deleteAtZero() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(0, 5, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos, "Delete from start should shift insert left");
        }

        @Test
        @DisplayName("Insert at position 0, delete after")
        void insertAtZero_deleteAfter() {
            InsertOp insert = OperationFixtures.createInsert(0, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(0, transformed.pos, "Insert at 0 before delete should not change");
        }

        @Test
        @DisplayName("Very large positions")
        void largePositions() {
            InsertOp insert = OperationFixtures.createInsert(1_000_000, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(500_000, 100_000, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(900_000, transformed.pos);
        }

        @Test
        @DisplayName("Empty text insert still transforms correctly")
        void emptyText_transformsCorrectly() {
            InsertOp insert = OperationFixtures.createInsert(10, "", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(7, transformed.pos);
            assertEquals("", transformed.text);
        }

        @Test
        @DisplayName("Null text insert transforms correctly")
        void nullText_transformsCorrectly() {
            InsertOp insert = OperationFixtures.createInsert(10, null, "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(7, transformed.pos);
            assertNull(transformed.text);
        }

        @Test
        @DisplayName("Null clientId handled gracefully")
        void nullClientId_handled() {
            InsertOp insert = OperationFixtures.createInsert(10, "X", null, "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            assertDoesNotThrow(() -> {
                OtTransform.transform(insert, delete);
            });
        }
    }

    @Nested
    @DisplayName("Algorithm correctness (math verification)")
    class AlgorithmCorrectness {

        @Test
        @DisplayName("Shift formula: pos - min(deleteLen, pos - deletePos)")
        void verifyShiftFormula() {
            // Test case: insert at 20, delete at 10 with length 5
            // Expected: 20 - min(5, 20-10) = 20 - min(5, 10) = 20 - 5 = 15

            InsertOp insert = OperationFixtures.createInsert(20, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(10, 5, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            int expectedPos = 20 - Math.min(5, 20 - 10);
            assertEquals(expectedPos, transformed.pos,
                    "Should follow formula: pos - min(deleteLen, pos - deletePos)");
        }

        @Test
        @DisplayName("No shift when condition pos > deletePos is false")
        void noShift_whenConditionFalse() {
            InsertOp insert = OperationFixtures.createInsert(5, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 10, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertEquals(5, transformed.pos,
                    "Should not shift when pos <= deletePos");
        }

        @Test
        @DisplayName("Min function prevents negative positions")
        void minFunction_preventsNegative() {
            InsertOp insert = OperationFixtures.createInsert(7, "X", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 100, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(insert, delete);

            assertTrue(transformed.pos >= 0, "Position should never become negative");
            assertEquals(5, transformed.pos, "Should shift by min(100, 2) = 2, resulting in position 5");
        }
    }
}