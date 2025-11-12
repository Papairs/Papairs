package com.papairs.docs.unit.ot;

import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.model.OT.Op;
import com.papairs.docs.unit.fixture.OperationFixtures;
import com.papairs.docs.util.OtTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OT Transform: Delete + Insert")
class OtTransformDeleteInsertTest {

    @Nested
    @DisplayName("When delete is after insert position")
    class DeleteAfterInsert {

        @Test
        @DisplayName("Delete after insert → delete position shifts right by insert length")
        void deleteAfter_shiftsRight() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Hello", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(15, transformed.pos,
                    "Delete position should shift right by insert text length");
            assertEquals(3, transformed.length,
                    "Delete length should remain unchanged");
        }

        @Test
        @DisplayName("Delete far after insert → shifts by full insert length")
        void deleteFarAfter_shiftsCorrectly() {
            DeleteOp opA = OperationFixtures.createDelete(100, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(10, "ABC", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(103, transformed.pos, "100 + 3");
            assertEquals(5, transformed.length);
        }

        @ParameterizedTest(name = "Delete at {0}, Insert at {1} with length {2} → Delete shifts to {3}")
        @CsvSource({
                "10, 0, 5, 15",    // Insert at start
                "10, 5, 3, 13",    // Insert before delete
                "10, 9, 1, 11",    // Insert just before delete
                "50, 20, 10, 60",  // Larger positions
                "100, 99, 1, 101"  // Insert just before
        })
        @DisplayName("Various positions with delete after insert")
        void variousPositions_deleteAfter(int deletePos, int insertPos, int insertLen, int expectedPos) {
            String text = "X".repeat(insertLen);
            DeleteOp opA = OperationFixtures.createDelete(deletePos, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(insertPos, text, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(expectedPos, transformed.pos);
        }
    }

    @Nested
    @DisplayName("When delete is at same position as insert")
    class DeleteAtSamePosition {

        @Test
        @DisplayName("Delete at same position as insert → delete shifts right")
        void deleteAtSamePosition_shiftsRight() {
            DeleteOp opA = OperationFixtures.createDelete(5, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Hi", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(7, transformed.pos, "Delete at same position should shift right (>= condition)");
            assertEquals(3, transformed.length);
        }

        @Test
        @DisplayName("Delete at position 0, Insert at position 0 → delete shifts")
        void bothAtZero_deleteShifts() {
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(0, "Start", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos, "0 + 5");
        }
    }

    @Nested
    @DisplayName("When delete is before insert position")
    class DeleteBeforeInsert {

        @Test
        @DisplayName("Delete before insert → delete position unchanged")
        void deleteBefore_noShift() {
            DeleteOp opA = OperationFixtures.createDelete(5, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(10, "Hello", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos, "Delete before insert should not shift");
            assertEquals(3, transformed.length);
        }

        @Test
        @DisplayName("Delete at start, insert at end → no shift")
        void deleteAtStart_insertAtEnd() {
            DeleteOp opA = OperationFixtures.createDelete(0, 2, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(100, "End", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos);
        }

        @ParameterizedTest(name = "Delete at {0}, Insert at {1} → Delete stays at {0}")
        @CsvSource({
                "0, 10",
                "5, 10",
                "5, 6",     // Just one position apart
                "50, 100",
                "99, 100"
        })
        @DisplayName("Various positions with delete before insert")
        void variousPositions_deleteBefore(int deletePos, int insertPos) {
            DeleteOp opA = OperationFixtures.createDelete(deletePos, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(insertPos, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(deletePos, transformed.pos, "Delete position should not change when before insert");
        }
    }

    @Nested
    @DisplayName("Insert text length variations")
    class InsertTextLength {

        @Test
        @DisplayName("Empty insert text (length 0) → no shift")
        void emptyInsertText_noShift() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(10, transformed.pos, "Empty insert should not shift delete position");
        }

        @Test
        @DisplayName("Null insert text treated as empty → no shift")
        void nullInsertText_noShift() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, null, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(10, transformed.pos, "Null insert text should be treated as empty");
        }

        @ParameterizedTest(name = "Insert length {0} shifts delete by {0}")
        @CsvSource({
                "1, 11",
                "5, 15",
                "10, 20",
                "50, 60",
                "100, 110"
        })
        @DisplayName("Various insert lengths shift delete correctly")
        void variousInsertLengths(int insertLength, int expectedDeletePos) {
            String text = "X".repeat(insertLength);
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, text, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(expectedDeletePos, transformed.pos);
        }

        @Test
        @DisplayName("Very long insert text shifts correctly")
        void veryLongInsert_shiftsCorrectly() {
            String longText = "X".repeat(1000);
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, longText, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(1010, transformed.pos, "10 + 1000");
        }

        @Test
        @DisplayName("Unicode characters shift by string length")
        void unicodeCharacters_shiftsByStringLength() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "😀😀😀", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(10 + "😀😀😀".length(), transformed.pos, "Should shift by Java string length");
        }
    }

    @Nested
    @DisplayName("Delete length variations")
    class DeleteLength {

        @Test
        @DisplayName("Delete length 1 remains unchanged after transform")
        void deleteLength_one() {
            DeleteOp opA = OperationFixtures.createDelete(10, 1, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(1, transformed.length, "Delete length should never change for delete+insert");
        }

        @ParameterizedTest(name = "Delete length {0} preserved")
        @CsvSource({
                "1",
                "5",
                "10",
                "50",
                "100"
        })
        @DisplayName("Various delete lengths remain unchanged")
        void variousDeleteLengths_preserved(int deleteLength) {
            DeleteOp opA = OperationFixtures.createDelete(10, deleteLength, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(deleteLength, transformed.length, "Delete length should always be preserved");
        }

        @Test
        @DisplayName("Zero-length delete (no-op) remains zero")
        void zeroLengthDelete_remainsZero() {
            DeleteOp opA = OperationFixtures.createDelete(10, 0, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.length);
        }
    }

    @Nested
    @DisplayName("Metadata preservation")
    class MetadataPreservation {

        @Test
        @DisplayName("clientId and opId remain unchanged")
        void metadata_preserved() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "myClient", "myOp456");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "otherClient", "otherOp");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals("myClient", transformed.clientId);
            assertEquals("myOp456", transformed.opId);
        }

        @Test
        @DisplayName("baseVersion remains unchanged")
        void baseVersion_preserved() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            opA.baseVersion = 99;
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(99, transformed.baseVersion);
        }

        @Test
        @DisplayName("type field remains 'delete'")
        void type_remainsDelete() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals("delete", transformed.type);
        }

        @Test
        @DisplayName("All delete properties preserved when delete is before insert")
        void allProperties_preservedWhenDeleteBefore() {
            DeleteOp opA = OperationFixtures.createDelete(5, 7, "myClient", "myOp");
            opA.baseVersion = 42;
            InsertOp opB = OperationFixtures.createInsert(20, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos);
            assertEquals(7, transformed.length);
            assertEquals("myClient", transformed.clientId);
            assertEquals("myOp", transformed.opId);
            assertEquals(42, transformed.baseVersion);
            assertEquals("delete", transformed.type);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Delete at position 0, insert before it (impossible but handled)")
        void deleteAtZero_insertAtZero() {
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(0, "Start", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos, "Delete at 0 with insert at 0 should shift (>= condition)");
        }

        @Test
        @DisplayName("Very large positions")
        void largePositions() {
            DeleteOp opA = OperationFixtures.createDelete(1_000_000, 10, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(999_999, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(1_000_004, transformed.pos, "1000000 + 4");
        }

        @Test
        @DisplayName("Boundary case: delete just after insert")
        void deleteJustAfterInsert() {
            DeleteOp opA = OperationFixtures.createDelete(6, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "X", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(7, transformed.pos, "6 + 1");
        }

        @Test
        @DisplayName("Boundary case: delete just before insert")
        void deleteJustBeforeInsert() {
            DeleteOp opA = OperationFixtures.createDelete(5, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(6, "X", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos, "Delete before insert, no shift");
        }

        @Test
        @DisplayName("Null clientId handled gracefully")
        void nullClientId_handled() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, null, "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            assertDoesNotThrow(() -> {
                DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);
                assertNull(transformed.clientId);
            });
        }

        @Test
        @DisplayName("Null opId handled gracefully")
        void nullOpId_handled() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", null);
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            assertDoesNotThrow(() -> {
                DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);
                assertNull(transformed.opId);
            });
        }

        @Test
        @DisplayName("Null operation B returns A unchanged")
        void nullOperationB_returnsUnchanged() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");

            Op transformed = OtTransform.transform(opA, null);

            assertEquals(opA, transformed, "Should return same reference when B is null");
        }
    }

    @Nested
    @DisplayName("Semantic correctness (intent preservation)")
    class SemanticCorrectness {

        @Test
        @DisplayName("Delete maintains its target after insert pushes content")
        void deleteTargetMaintained() {
            // Document: "Hello World"
            // Delete wants to remove "World" at position 6
            // Insert adds "Beautiful " at position 6

            DeleteOp deleteWorld = OperationFixtures.createDelete(6, 5, "client1", "op1");
            InsertOp insertBeautiful = OperationFixtures.createInsert(6, "Beautiful ", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(deleteWorld, insertBeautiful);

            // Delete should now target position 16 (6 + 10) to still hit "World"
            assertEquals(16, transformed.pos);
            assertEquals(5, transformed.length);
        }

        @Test
        @DisplayName("Delete before insert maintains independence")
        void deleteBeforeInsert_independent() {
            // Document: "Hello World"
            // Delete removes "Hello" at position 0
            // Insert adds "!" at position 8

            DeleteOp deleteHello = OperationFixtures.createDelete(0, 5, "client1", "op1");
            InsertOp insertExclaim = OperationFixtures.createInsert(8, "!", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(deleteHello, insertExclaim);

            // Delete should stay at 0 since insert is after
            assertEquals(0, transformed.pos);
            assertEquals(5, transformed.length);
        }
    }

    @Nested
    @DisplayName("Convergence properties")
    class ConvergenceProperties {

        @Test
        @DisplayName("Transform is deterministic (same inputs produce same output)")
        void transform_isDeterministic() {
            DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Text", "client2", "op2");

            DeleteOp result1 = (DeleteOp) OtTransform.transform(opA, opB);
            DeleteOp result2 = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(result1.pos, result2.pos);
            assertEquals(result1.length, result2.length);
        }

        @Test
        @DisplayName("Delete position adjustment is consistent with insert length")
        void positionAdjustment_consistentWithInsertLength() {
            for (int len = 1; len <= 10; len++) {
                DeleteOp opA = OperationFixtures.createDelete(10, 3, "client1", "op1");
                String text = "X".repeat(len);
                InsertOp opB = OperationFixtures.createInsert(5, text, "client2", "op2");

                DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

                assertEquals(10 + len, transformed.pos, "Position shift should equal insert length");
            }
        }

        @Test
        @DisplayName("Transform preserves delete semantics (what gets deleted)")
        void transformPreservesDeleteSemantics() {
            // If delete is at position X with length L,
            // after transform it should still delete the same content
            // (even though position may have shifted)

            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(8, "ABC", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            // Original would delete chars 10-14
            // After "ABC" inserted at 8, those chars are now at 13-17
            assertEquals(13, transformed.pos);
            assertEquals(5, transformed.length);
        }
    }

    @Nested
    @DisplayName("Interaction patterns")
    class InteractionPatterns {

        @Test
        @DisplayName("Multiple inserts before delete accumulate shifts")
        void multipleInserts_accumulateShifts() {
            DeleteOp delete = OperationFixtures.createDelete(10, 3, "client1", "op1");

            // Apply multiple inserts before the delete
            InsertOp insert1 = OperationFixtures.createInsert(5, "A", "client2", "op2");
            InsertOp insert2 = OperationFixtures.createInsert(6, "B", "client3", "op3");
            InsertOp insert3 = OperationFixtures.createInsert(7, "C", "client4", "op4");

            DeleteOp step1 = (DeleteOp) OtTransform.transform(delete, insert1);
            DeleteOp step2 = (DeleteOp) OtTransform.transform(step1, insert2);
            DeleteOp step3 = (DeleteOp) OtTransform.transform(step2, insert3);

            // Should shift by total of 3
            assertEquals(13, step3.pos, "10 + 1 + 1 + 1");
        }

        @Test
        @DisplayName("Insert after delete range doesn't affect delete")
        void insertAfterDeleteRange_noEffect() {
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client1", "op1"); // Deletes 5-7
            InsertOp insert = OperationFixtures.createInsert(20, "Text", "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(delete, insert);

            assertEquals(5, transformed.pos, "Insert after delete, no shift");
            assertEquals(3, transformed.length);
        }
    }
}