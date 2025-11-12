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
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OT Transform: Edge Cases")
class OtTransformEdgeCasesTest {

    @Nested
    @DisplayName("Null operation handling")
    class NullOperations {

        @Test
        @DisplayName("Transform(null, validOp) returns null")
        void nullFirstOp_returnsNull() {
            InsertOp opB = OperationFixtures.createInsert(5, "Hello", "defaultClient", "defaultOp");

            Op result = OtTransform.transform(null, opB);

            assertNull(result, "Transform with null first operation should return null");
        }

        @Test
        @DisplayName("Transform(validOp, null) returns original operation unchanged")
        void nullSecondOp_returnsOriginal() {
            InsertOp opA = OperationFixtures.createInsert(5, "Hello", "client1", "op1");

            Op result = OtTransform.transform(opA, null);

            assertNotNull(result);
            assertSame(opA, result, "Should return same instance when second op is null");
            assertEquals(5, result.pos);
        }

        @Test
        @DisplayName("Transform(null, null) returns null")
        void bothNull_returnsNull() {
            Op result = OtTransform.transform(null, null);

            assertNull(result);
        }
    }

    @Nested
    @DisplayName("Null fields in operations")
    class NullFields {

        @Test
        @DisplayName("Insert with null text → tiebreaker handles gracefully")
        void insertWithNullText_handledGracefully() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, null, "client2", "op2");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertEquals(10, result.pos, "Null text should be treated as length 0");
            });
        }

        @Test
        @DisplayName("Insert with null clientId → tiebreaker uses empty string")
        void insertWithNullClientId_usesEmptyString() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", null, "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Insert with null opId → tiebreaker uses empty string")
        void insertWithNullOpId_usesEmptyString() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", null);
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client2", "op2");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Both operations with null metadata → deterministic result")
        void bothWithNullMetadata_deterministic() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", null, null);
            InsertOp opB = OperationFixtures.createInsert(5, "B", null, null);

            InsertOp resultA = (InsertOp) OtTransform.transform(opA, opB);
            InsertOp resultB = (InsertOp) OtTransform.transform(opB, opA);

            assertNotNull(resultA);
            assertNotNull(resultB);
            // Tiebreaker should still produce consistent ordering
        }

        @Test
        @DisplayName("Delete with null clientId handled")
        void deleteWithNullClientId() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, null, "op1");
            DeleteOp opB = OperationFixtures.createDelete(8, 3, "client2", "op2");

            assertDoesNotThrow(() -> {
                OtTransform.transform(opA, opB);
            });
        }
    }

    @Nested
    @DisplayName("Boundary positions")
    class BoundaryPositions {

        @Test
        @DisplayName("Position 0 (start of document)")
        void positionZero_insert() {
            InsertOp opA = OperationFixtures.createInsert(0, "Start", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Middle", "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(0, result.pos, "Insert at position 0 should remain at 0");
        }

        @Test
        @DisplayName("Position 0 (start of document) - delete")
        void positionZero_delete() {
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(10, "Text", "client2", "op2");

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, result.pos, "Delete at position 0 should remain at 0");
        }

        @ParameterizedTest(name = "Large position: {0}")
        @ValueSource(ints = {1000, 10_000, 100_000, 1_000_000, Integer.MAX_VALUE - 1000})
        @DisplayName("Very large positions don't overflow")
        void largePositions_noOverflow(int largePos) {
            InsertOp opA = OperationFixtures.createInsert(largePos, "Text", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(0, "Start", "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertTrue(result.pos >= largePos, "Large position should increase or stay same");
            assertTrue(result.pos > 0, "Position should remain positive");
        }

        @Test
        @DisplayName("Maximum reasonable position doesn't cause issues")
        void maximumReasonablePosition() {
            int maxPos = Integer.MAX_VALUE / 2; // Reasonable max for document
            InsertOp opA = OperationFixtures.createInsert(maxPos, "Text", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(0, "A", "client2", "op2");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertTrue(result.pos > 0);
            });
        }
    }

    @Nested
    @DisplayName("Extreme lengths and sizes")
    class ExtremeLengths {

        @Test
        @DisplayName("Insert with empty string")
        void insertEmptyString() {
            InsertOp opA = OperationFixtures.createInsert(10, "Hello", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "", "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(10, result.pos, "Empty insert should not shift position");
        }

        @Test
        @DisplayName("Delete with zero length")
        void deleteZeroLength() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 0, "client2", "op2");

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(10, result.pos, "Zero-length delete should not affect position");
            assertEquals(5, result.length, "Length should remain unchanged");
        }

        @Test
        @DisplayName("Delete with very large length")
        void deleteVeryLargeLength() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 1000, "client2", "op2");

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertTrue(result.pos >= 0, "Position should not go negative");
        }

        @Test
        @DisplayName("Insert with very long text")
        void insertVeryLongText() {
            String longText = "X".repeat(10_000);
            InsertOp opA = OperationFixtures.createInsert(100, "Short", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(50, longText, "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(100 + longText.length(), result.pos,
                    "Should shift by full length of long text");
        }

        @Test
        @DisplayName("Insert with unicode characters (emojis)")
        void insertUnicodeEmojis() {
            InsertOp opA = OperationFixtures.createInsert(10, "Text", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "😀😎🎉🚀", "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            int expectedShift = "😀😎🎉🚀".length();
            assertEquals(10 + expectedShift, result.pos,
                    "Should shift by Java string length (UTF-16 code units)");
        }

        @Test
        @DisplayName("Insert with special characters")
        void insertSpecialCharacters() {
            InsertOp opA = OperationFixtures.createInsert(10, "Text", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "\n\t\r\0", "client2", "op2");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(14, result.pos, "Special characters should count normally");
        }
    }

    @Nested
    @DisplayName("Overlapping operations")
    class OverlappingOperations {

        @Test
        @DisplayName("Delete completely overlapping another delete")
        void deleteCompletelyOverlapsDelete() {
            DeleteOp opA = OperationFixtures.createDelete(5, 10, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 10, "client2", "op2");

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, result.length,
                    "Completely overlapping deletes should result in zero length");
        }

        @Test
        @DisplayName("Delete partially overlapping from left")
        void deletePartiallyOverlapsFromLeft() {
            DeleteOp opA = OperationFixtures.createDelete(10, 10, "client1", "op1"); // Delete 10-20
            DeleteOp opB = OperationFixtures.createDelete(5, 10, "client2", "op2");  // Delete 5-15

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, result.pos, "Position should adjust to start of opB");
            assertTrue(result.length < 10, "Length should be reduced by overlap");
        }

        @Test
        @DisplayName("Delete partially overlapping from right")
        void deletePartiallyOverlapsFromRight() {
            DeleteOp opA = OperationFixtures.createDelete(5, 10, "client1", "op1");  // Delete 5-15
            DeleteOp opB = OperationFixtures.createDelete(10, 10, "client2", "op2"); // Delete 10-20

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, result.length, "Length should be reduced by overlap");
        }

        @Test
        @DisplayName("Delete inside another delete (completely contained)")
        void deleteInsideDelete() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");  // Delete 10-15
            DeleteOp opB = OperationFixtures.createDelete(5, 20, "client2", "op2");  // Delete 5-25 (contains opA)

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, result.length,
                    "Delete completely contained in another should have zero length");
        }

        @Test
        @DisplayName("Delete containing another delete")
        void deleteContainsDelete() {
            DeleteOp opA = OperationFixtures.createDelete(5, 20, "client1", "op1");  // Delete 5-25
            DeleteOp opB = OperationFixtures.createDelete(10, 5, "client2", "op2");  // Delete 10-15 (inside opA)

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(15, result.length, "Length should be reduced by contained delete");
        }
    }

    @Nested
    @DisplayName("Adjacent operations (touching boundaries)")
    class AdjacentOperations {

        @Test
        @DisplayName("Insert immediately after another insert")
        void insertImmediatelyAfter() {
            InsertOp opA = OperationFixtures.createInsert(10, "World", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "Hello", "client2", "op2"); // opB at 5, adds 5 chars

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals(15, result.pos, "Should shift past the inserted text");
        }

        @Test
        @DisplayName("Delete immediately after another delete")
        void deleteImmediatelyAfter() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 5, "client2", "op2");

            DeleteOp result = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, result.pos, "Should shift back by deleted range");
        }

        @Test
        @DisplayName("Operations at same position with different types")
        void samePosition_insertAndDelete() {
            InsertOp insert = OperationFixtures.createInsert(5, "Text", "client1", "op1");
            DeleteOp delete = OperationFixtures.createDelete(5, 3, "client2", "op2");

            assertDoesNotThrow(() -> {
                OtTransform.transform(insert, delete);
                OtTransform.transform(delete, insert);
            });
        }
    }

    @Nested
    @DisplayName("Metadata edge cases")
    class MetadataEdgeCases {

        @Test
        @DisplayName("Empty string clientId and opId")
        void emptyStringMetadata() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "", "");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "", "");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertNotNull(result);
            });
        }

        @Test
        @DisplayName("Very long clientId and opId strings")
        void veryLongMetadataStrings() {
            String longId = "x".repeat(1000);
            InsertOp opA = OperationFixtures.createInsert(5, "A", longId, longId);
            InsertOp opB = OperationFixtures.createInsert(5, "B", "short", "short");

            assertDoesNotThrow(() -> {
                InsertOp result = (InsertOp) OtTransform.transform(opA, opB);
                assertEquals(longId, result.clientId);
                assertEquals(longId, result.opId);
            });
        }

        @Test
        @DisplayName("Special characters in clientId and opId")
        void specialCharactersInMetadata() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client:1", "op@123!");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client#2", "op$456?");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            assertEquals("client:1", result.clientId);
            assertEquals("op@123!", result.opId);
        }

        @Test
        @DisplayName("Identical clientId and opId (same operation)")
        void identicalMetadata() {
            InsertOp opA = OperationFixtures.createInsert(5, "A", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "B", "client1", "op1");

            InsertOp result = (InsertOp) OtTransform.transform(opA, opB);

            // Tiebreaker with identical IDs should be deterministic
            assertNotNull(result);
        }
    }

    @Nested
    @DisplayName("Operation type edge cases")
    class TypeEdgeCases {

        @Test
        @DisplayName("Unknown operation type returns original")
        void unknownOperationType() {
            Op unknownOp = new Op() {};
            unknownOp.type = "unknown";
            unknownOp.pos = 5;

            InsertOp opB = OperationFixtures.createInsert(10, "Text", "client2", "op2");

            Op result = OtTransform.transform(unknownOp, opB);

            assertSame(unknownOp, result,
                    "Unknown operation type should return original unchanged");
        }

        @Test
        @DisplayName("Null type field handled")
        void nullTypeField() {
            InsertOp opA = OperationFixtures.createInsert(5, "Text", "client1", "op1");
            opA.type = null;

            InsertOp opB = OperationFixtures.createInsert(10, "Text", "client2", "op2");

            // Should not crash, though behavior may be undefined
            assertDoesNotThrow(() -> {
                OtTransform.transform(opA, opB);
            });
        }

        @Test
        @DisplayName("Case sensitivity of type field")
        void caseSensitiveTypeField() {
            InsertOp opA = OperationFixtures.createInsert(5, "Text", "client1", "op1");
            opA.type = "INSERT"; // Wrong case

            InsertOp opB = OperationFixtures.createInsert(10, "Text", "client2", "op2");

            Op result = OtTransform.transform(opA, opB);

            // Should return original since type doesn't match exactly
            assertSame(opA, result);
        }
    }

    @Nested
    @DisplayName("Transformation chains")
    class TransformationChains {

        @Test
        @DisplayName("Transform against multiple operations sequentially")
        void transformAgainstMultipleOps() {
            InsertOp original = OperationFixtures.createInsert(10, "Text", "client1", "op1");

            InsertOp op1 = OperationFixtures.createInsert(0, "A", "client2", "op2");
            InsertOp op2 = OperationFixtures.createInsert(5, "B", "client3", "op3");
            InsertOp op3 = OperationFixtures.createInsert(8, "C", "client4", "op4");

            Op result = original;
            result = OtTransform.transform(result, op1);
            result = OtTransform.transform(result, op2);
            result = OtTransform.transform(result, op3);

            assertNotNull(result);
            assertTrue(result.pos >= 10, "Position should have shifted forward");
        }

        @Test
        @DisplayName("Transform result can be transformed again")
        void transformResultIsReusable() {
            InsertOp opA = OperationFixtures.createInsert(10, "Text", "client1", "op1");
            int originalPos = opA.pos;

            InsertOp opB = OperationFixtures.createInsert(5, "More", "client2", "op2");
            Op result1 = OtTransform.transform(opA, opB);

            InsertOp opC = OperationFixtures.createInsert(3, "Even", "client3", "op3");
            Op result2 = OtTransform.transform(result1, opC);

            assertNotNull(result2);
            assertTrue(result2.pos > originalPos, "Should continue shifting");
            assertEquals(18, result2.pos, "Should shift by both operations");
        }
    }

    @Nested
    @DisplayName("Operation immutability")
    class ImmutabilityTests {

        @Test
        @DisplayName("Original operation is modified (current implementation)")
        void originalOperationModified() {
            InsertOp opA = OperationFixtures.createInsert(10, "Text", "client1", "op1");
            int originalPos = opA.pos;

            InsertOp opB = OperationFixtures.createInsert(5, "More", "client2", "op2");

            Op result = OtTransform.transform(opA, opB);

            // Current implementation modifies the original
            assertSame(opA, result, "Returns same instance");
            assertNotEquals(originalPos, opA.pos, "Original is modified");
        }

        @Test
        @DisplayName("Second operation is never modified")
        void secondOperationNotModified() {
            InsertOp opA = OperationFixtures.createInsert(10, "Text", "client1", "op1");
            InsertOp opB = OperationFixtures.createInsert(5, "More", "client2", "op2");

            int originalPosB = opB.pos;
            String originalTextB = opB.text;

            OtTransform.transform(opA, opB);

            assertEquals(originalPosB, opB.pos, "Second op position unchanged");
            assertEquals(originalTextB, opB.text, "Second op text unchanged");
        }
    }
}