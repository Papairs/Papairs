package com.papairs.docs.unit.ot;

import com.papairs.docs.model.OT.InsertOp;
import com.papairs.docs.unit.fixture.OperationFixtures;
import com.papairs.docs.util.OtTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OT Transform: Insert + Insert")
class OtTransformInsertInsertTest {

    @Nested
    @DisplayName("When operations have different positions")
    class DifferentPositions {

        @Test
        @DisplayName("Insert A after Insert B position → A position shifts right")
        void insertAfter_shiftsRight() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "World", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(15, transformed.pos, "Position should shift by length of opB text");
            assertEquals("Hello", transformed.text, "Text should remain unchanged");
        }

        @Test
        @DisplayName("Insert A before Insert B position → A position unchanged")
        void insertBefore_noShift() {
            InsertOp opA = OperationFixtures.createInsert(5, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(10, "World", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos, "Position should not change when inserting before opB");
            assertEquals("Hello", transformed.text);
        }

        @ParameterizedTest(name = "A at {0}, B at {1} → A shifts to {2}")
        @CsvSource({
                "10, 0, 15",    // B at start
                "10, 5, 15",    // B before A
                "10, 9, 15",    // B just before A
                "0, 5, 0",      // A at start, B after
                "100, 50, 105"  // Large positions
        })
        @DisplayName("Various position combinations shift correctly")
        void variousPositions_transformCorrectly(int posA, int posB, int expectedPos) {
            InsertOp opA = OperationFixtures.createInsert(posA, "XXXXX", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(posB, "YYYYY", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(expectedPos, transformed.pos);
        }
    }

    @Nested
    @DisplayName("When operations have same position (tiebreaker)")
    class SamePositionTiebreaker {

        @Test
        @DisplayName("Same position → tiebreaker determines order")
        void samePosition_usesTiebreaker() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            InsertOp transformedA = (InsertOp) OtTransform.transform(opA, opB);
            InsertOp transformedB = (InsertOp) OtTransform.transform(opB, opA);

            assertNotEquals(transformedA.pos, transformedB.pos, "Tiebreaker should cause one operation to shift");

            // One should stay at 5, other should shift
            assertTrue(
                    (transformedA.pos == 5 && transformedB.pos == 6) ||
                            (transformedA.pos == 6 && transformedB.pos == 5),
                    "One op should stay at 5, other should shift to 6"
            );
        }

        @Test
        @DisplayName("Tiebreaker is deterministic (alphabetical by clientId:opId)")
        void tiebreaker_isDeterministic() {
            // "client1:op1" < "client2:op2" alphabetically
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            InsertOp transformedA = (InsertOp) OtTransform.transform(opA, opB);

            // client1:op1 wins (smaller alphabetically), so opA stays put
            assertEquals(5, transformedA.pos, "Lower alphabetical clientId:opId should not shift");
        }

        @Test
        @DisplayName("Tiebreaker handles same clientId, different opId")
        void tiebreaker_sameClient_differentOpId() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", "op999");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client1", "op100");

            InsertOp transformedA = (InsertOp) OtTransform.transform(opA, opB);

            // "client1:op100" < "client1:op999" alphabetically
            assertEquals(6, transformedA.pos, "Operation with higher opId should shift");
        }

        @Test
        @DisplayName("Tiebreaker is symmetric (transform(A,B) opposite of transform(B,A))")
        void tiebreaker_isSymmetric() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "alice", "1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "bob", "2");

            InsertOp transformedA = (InsertOp) OtTransform.transform(opA, opB);
            InsertOp transformedB = (InsertOp) OtTransform.transform(opB, opA);

            assertEquals(5, transformedA.pos);
            assertEquals(6, transformedB.pos);

            // Reverse should give opposite result
            InsertOp reverseA = (InsertOp) OtTransform.transform(opB, opA);
            InsertOp reverseB = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(transformedB.pos, reverseA.pos);
            assertEquals(transformedA.pos, reverseB.pos);
        }
    }

    @Nested
    @DisplayName("Text length variations")
    class TextLength {

        @Test
        @DisplayName("Empty text insert (length 0) → no shift")
        void emptyText_noShift() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(10, transformed.pos, "Empty text should not shift position");
        }

        @Test
        @DisplayName("Null text treated as empty → no shift")
        void nullText_treatedAsEmpty() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, null, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(10, transformed.pos, "Null text should be treated as empty");
        }

        @ParameterizedTest(name = "Text length {0} shifts by {0}")
        @CsvSource({
                "1, 11",
                "5, 15",
                "10, 20",
                "100, 110"
        })
        @DisplayName("Various text lengths shift correctly")
        void variousTextLengths_shiftCorrectly(int textLength, int expectedPos) {
            String text = "X".repeat(textLength);
            InsertOp opA = OperationFixtures.createInsert(10, "Test", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, text, "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(expectedPos, transformed.pos);
        }

        @Test
        @DisplayName("Multi-byte unicode characters count correctly")
        void unicodeCharacters_countCorrectly() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "😀😀😀", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            // Should shift by string length (6 for 3 emojis in Java UTF-16)
            assertEquals(10 + "😀😀😀".length(), transformed.pos,
                    "Should shift by Java string length");
        }
    }

    @Nested
    @DisplayName("Metadata preservation")
    class MetadataPreservation {

        @Test
        @DisplayName("clientId and opId remain unchanged")
        void metadata_preserved() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "myClient", "myOp123");
            InsertOp opB = OperationFixtures.createInsert(5, "World", "otherClient", "otherOp");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals("myClient", transformed.clientId);
            assertEquals("myOp123", transformed.opId);
        }

        @Test
        @DisplayName("baseVersion remains unchanged")
        void baseVersion_preserved() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            opA.baseVersion = 42;
            InsertOp opB = OperationFixtures.createInsert(5, "World", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(42, transformed.baseVersion);
        }

        @Test
        @DisplayName("type field remains 'insert'")
        void type_remainsInsert() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "World", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals("insert", transformed.type);
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Position 0 (start of document)")
        void position_zero() {
            InsertOp opA = OperationFixtures.createInsert(5, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(0, "Start", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(10, transformed.pos); // 5 + 5
        }

        @Test
        @DisplayName("Very large positions")
        void largePositions() {
            InsertOp opA = OperationFixtures.createInsert(1_000_000, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(999_999, "World", "client2", "op2");

            InsertOp transformed = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(1_000_005, transformed.pos);
        }

        @Test
        @DisplayName("Null clientId handled by tiebreaker")
        void nullClientId_handled() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", null, "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            // Should not throw, tiebreaker treats null as empty string
            assertDoesNotThrow(() -> {
                OtTransform.transform(opA, opB);
            });
        }

        @Test
        @DisplayName("Null opId handled by tiebreaker")
        void nullOpId_handled() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", null);
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            assertDoesNotThrow(() -> {
                OtTransform.transform(opA, opB);
            });
        }
    }

    @Nested
    @DisplayName("Convergence properties")
    class ConvergenceProperties {

        @Test
        @DisplayName("Transform(A,B) + Transform(B,A) should commute")
        void convergence_insertInsert_twoWay() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(8, "B", "client2", "op2");

            // Client 1's view: A then B'
            InsertOp bPrime = (InsertOp) OtTransform.transform(opB, opA);

            // Client 2's view: B then A'
            InsertOp aPrime = (InsertOp) OtTransform.transform(opA, opB);

            // Both should have same relative ordering
            // A at 5, B at 8 → after transform:
            // Client 1: A at 5, B' at 9 (8+1)
            // Client 2: B at 8, A' at 5

            assertEquals(9, bPrime.pos);
            assertEquals(5, aPrime.pos);

            assertNotEquals(aPrime.pos, bPrime.pos);
        }
    }
}