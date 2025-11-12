package com.papairs.docs.unit.ot;

import com.papairs.docs.model.OT.DeleteOp;
import com.papairs.docs.unit.fixture.OperationFixtures;
import com.papairs.docs.util.OtTransform;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("OT Transform: Delete + Delete")
class OtTransformDeleteDeleteTest {

    @Nested
    @DisplayName("When deletes do not overlap")
    class NonOverlapping {

        @Test
        @DisplayName("Delete B completely before Delete A → A shifts left by B length")
        void deleteB_completelyBefore_shiftsALeft() {
            // Document: "Hello World"
            // B deletes "Hello" at pos 0, length 5
            // A deletes "World" at pos 6, length 5
            DeleteOp opA = OperationFixtures.createDelete(6, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(1, transformed.pos,
                    "Position should shift left by B's length (6 - 5)");
            assertEquals(5, transformed.length,
                    "Length should remain unchanged when no overlap");
        }

        @Test
        @DisplayName("Delete B completely after Delete A → A unchanged")
        void deleteB_completelyAfter_noChange() {
            // Document: "Hello World"
            // A deletes "Hello" at pos 0, length 5
            // B deletes "World" at pos 6, length 5
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(6, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos,
                    "Position should not change when B is after A");
            assertEquals(5, transformed.length,
                    "Length should not change when B is after A");
        }

        @Test
        @DisplayName("Delete B adjacent before A → A shifts exactly to B's position")
        void deleteB_adjacentBefore_shiftsToB() {
            // Document: "HelloWorld"
            // B deletes "Hello" at pos 0, length 5
            // A deletes "World" at pos 5, length 5
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos,
                    "Adjacent deletes: A should start at B's position");
            assertEquals(5, transformed.length,
                    "Length unchanged for adjacent non-overlapping");
        }

        @Test
        @DisplayName("Delete B adjacent after A → A unchanged")
        void deleteB_adjacentAfter_noChange() {
            // Document: "HelloWorld"
            // A deletes "Hello" at pos 0, length 5
            // B deletes "World" at pos 5, length 5
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos);
            assertEquals(5, transformed.length);
        }

        @ParameterizedTest(name = "A[{0},{1}] B[{2},{3}] → A''[{4},{5}]")
        @CsvSource({
                "10, 5, 0, 5, 5, 5",      // B before A, gap between
                "10, 5, 0, 10, 0, 5",     // B before A, adjacent
                "10, 5, 0, 3, 7, 5",      // B before A, different lengths
                "0, 5, 10, 5, 0, 5",      // B after A
                "0, 5, 20, 10, 0, 5"      // B far after A
        })
        @DisplayName("Various non-overlapping positions")
        void nonOverlapping_variousPositions(
                int aPos, int aLen, int bPos, int bLen,
                int expectedPos, int expectedLen) {
            DeleteOp opA = OperationFixtures.createDelete(aPos, aLen, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(bPos, bLen, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(expectedPos, transformed.pos);
            assertEquals(expectedLen, transformed.length);
        }
    }

    @Nested
    @DisplayName("When deletes overlap")
    class Overlapping {

        @Test
        @DisplayName("Partial overlap from left → reduce A length, keep A position")
        void partialOverlap_fromLeft() {
            // Document: "Hello World"
            // A deletes positions 5-10 (length 5)
            // B deletes positions 3-7 (length 4)
            // Overlap is positions 5-7 (length 2)
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(3, 4, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(3, transformed.pos,
                    "Position should move to B's start when B starts before A");
            assertEquals(3, transformed.length,
                    "Length should reduce by overlap (5 - 2)");
        }

        @Test
        @DisplayName("Partial overlap from right → reduce A length, keep A position")
        void partialOverlap_fromRight() {
            // Document: "Hello World"
            // A deletes positions 3-7 (length 4)
            // B deletes positions 5-10 (length 5)
            // Overlap is positions 5-7 (length 2)
            DeleteOp opA = OperationFixtures.createDelete(3, 4, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(3, transformed.pos,
                    "Position should stay when A starts before B");
            assertEquals(2, transformed.length,
                    "Length should reduce by overlap (4 - 2)");
        }

        @Test
        @DisplayName("B completely contains A → A length becomes 0")
        void deleteB_containsA_lengthBecomesZero() {
            // Document: "Hello World"
            // A deletes positions 3-7 (length 4)
            // B deletes positions 0-10 (length 10) - contains A
            DeleteOp opA = OperationFixtures.createDelete(3, 4, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 10, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos,
                    "Position should move to B's start");
            assertEquals(0, transformed.length,
                    "Length should be 0 when fully overlapped");
        }

        @Test
        @DisplayName("A completely contains B → A length reduces by B length")
        void deleteA_containsB_lengthReducesByB() {
            // Document: "Hello World"
            // A deletes positions 0-10 (length 10)
            // B deletes positions 3-7 (length 4) - contained in A
            DeleteOp opA = OperationFixtures.createDelete(0, 10, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(3, 4, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos,
                    "Position stays when A starts before B");
            assertEquals(6, transformed.length,
                    "Length reduces by B's length (10 - 4)");
        }

        @Test
        @DisplayName("Identical deletes → A length becomes 0")
        void identicalDeletes_lengthBecomesZero() {
            // Both delete same region
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos,
                    "Position unchanged for identical start");
            assertEquals(0, transformed.length,
                    "Length should be 0 when deletes are identical");
        }

        @ParameterizedTest(name = "A[{0},{1}] B[{2},{3}] → overlap={4}, result[{5},{6}]")
        @CsvSource({
                // aPos, aLen, bPos, bLen, expectedOverlap, expectedPos, expectedLen
                "5, 5, 3, 4, 2, 3, 3",        // Left partial overlap
                "3, 4, 5, 5, 2, 3, 2",        // Right partial overlap
                "5, 5, 5, 3, 3, 5, 2",        // B starts same, ends inside
                "5, 5, 7, 3, 3, 5, 2",        // B starts inside, ends inside
                "5, 10, 6, 3, 3, 5, 7",       // B fully inside A
                "5, 5, 0, 20, 5, 0, 0",       // B completely contains A
                "5, 5, 5, 5, 5, 5, 0"         // Identical
        })
        @DisplayName("Various overlapping scenarios")
        void overlapping_variousScenarios(
                int aPos, int aLen, int bPos, int bLen,
                int expectedOverlap, int expectedPos, int expectedLen) {
            DeleteOp opA = OperationFixtures.createDelete(aPos, aLen, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(bPos, bLen, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(expectedPos, transformed.pos,
                    String.format("Position mismatch for A[%d,%d] B[%d,%d]",
                            aPos, aLen, bPos, bLen));
            assertEquals(expectedLen, transformed.length,
                    String.format("Length mismatch for A[%d,%d] B[%d,%d]",
                            aPos, aLen, bPos, bLen));
        }
    }

    @Nested
    @DisplayName("Position adjustment rules")
    class PositionAdjustment {

        @Test
        @DisplayName("When B starts before A → A position moves to B's start")
        void bStartsBeforeA_positionMovesToB() {
            // A deletes [10, 15), B deletes [5, 12)
            // Overlap is [10, 12), length 2
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 7, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos,
                    "When B.pos < A.pos and overlap exists, A.pos = B.pos");
        }

        @Test
        @DisplayName("When A starts before B → A position stays")
        void aStartsBeforeB_positionStays() {
            // A deletes [5, 12), B deletes [10, 15)
            // Overlap is [10, 12), length 2
            DeleteOp opA = OperationFixtures.createDelete(5, 7, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(10, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos,
                    "When A.pos < B.pos, A.pos stays unchanged");
        }

        @Test
        @DisplayName("When same start position → position unchanged")
        void sameStartPosition_positionUnchanged() {
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 3, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos);
        }
    }

    @Nested
    @DisplayName("Length adjustment rules")
    class LengthAdjustment {

        @Test
        @DisplayName("Overlap calculation: max(0, min(endA, endB) - max(startA, startB))")
        void overlapCalculation_formula() {
            // A: [5, 10) = pos 5, length 5
            // B: [7, 12) = pos 7, length 5
            // overlap = min(10, 12) - max(5, 7) = 10 - 7 = 3
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(7, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(2, transformed.length,
                    "Length should be original minus overlap (5 - 3)");
        }

        @Test
        @DisplayName("No overlap → length unchanged")
        void noOverlap_lengthUnchanged() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.length);
        }

        @Test
        @DisplayName("Full overlap → length becomes 0")
        void fullOverlap_lengthBecomesZero() {
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(3, 10, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.length,
                    "Fully overlapped delete should have length 0");
        }

        @Test
        @DisplayName("Partial overlap reduces length proportionally")
        void partialOverlap_reducesLengthProportionally() {
            // A: [10, 20) length 10
            // B: [15, 25) length 10
            // Overlap: [15, 20) length 5
            // Result: length should be 5
            DeleteOp opA = OperationFixtures.createDelete(10, 10, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(15, 10, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.length,
                    "Should reduce by exactly the overlap amount");
        }
    }

    @Nested
    @DisplayName("Edge cases")
    class EdgeCases {

        @Test
        @DisplayName("Zero length delete → no transformation effect")
        void zeroLength_noEffect() {
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(3, 0, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos,
                    "Zero-length delete should not affect position");
            assertEquals(5, transformed.length,
                    "Zero-length delete should not affect length");
        }

        @Test
        @DisplayName("Delete at position 0")
        void deleteAtStart() {
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos);
            assertEquals(5, transformed.length);
        }

        @Test
        @DisplayName("Very large positions and lengths")
        void largeValues() {
            DeleteOp opA = OperationFixtures.createDelete(1_000_000, 1_000, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(999_500, 1_000, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            // B ends at 1_000_500, overlap with A [1_000_000, 1_001_000)
            // Overlap = 500
            assertEquals(999_500, transformed.pos);
            assertEquals(500, transformed.length);
        }

        @Test
        @DisplayName("Single character deletes")
        void singleCharacterDeletes() {
            DeleteOp opA = OperationFixtures.createDelete(5, 1, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(4, 1, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(4, transformed.pos);
            assertEquals(1, transformed.length);
        }

        @Test
        @DisplayName("Adjacent single character deletes merge position")
        void adjacentSingleChar_mergePosition() {
            DeleteOp opA = OperationFixtures.createDelete(5, 1, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 1, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(5, transformed.pos);
            assertEquals(0, transformed.length,
                    "Deleting same character results in zero length");
        }

        @Test
        @DisplayName("Negative overlap clamped to zero")
        void negativeOverlap_clampedToZero() {
            // This shouldn't happen in practice, but the code uses Math.max(0, ...)
            // to guard against it
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(20, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            // No overlap, so A unchanged
            assertEquals(10, transformed.pos);
            assertEquals(5, transformed.length);
        }
    }

    @Nested
    @DisplayName("Metadata preservation")
    class MetadataPreservation {

        @Test
        @DisplayName("clientId and opId remain unchanged")
        void metadata_preserved() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "myClient", "myOp123");
            DeleteOp opB = OperationFixtures.createDelete(5, 3, "otherClient", "otherOp");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals("myClient", transformed.clientId);
            assertEquals("myOp123", transformed.opId);
        }

        @Test
        @DisplayName("baseVersion remains unchanged")
        void baseVersion_preserved() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            opA.baseVersion = 42;
            DeleteOp opB = OperationFixtures.createDelete(5, 3, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(42, transformed.baseVersion);
        }

        @Test
        @DisplayName("type field remains 'delete'")
        void type_remainsDelete() {
            DeleteOp opA = OperationFixtures.createDelete(10, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 3, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals("delete", transformed.type);
        }
    }

    @Nested
    @DisplayName("Convergence properties")
    class ConvergenceProperties {

        @Test
        @DisplayName("Transformation preserves intent despite conflict")
        void preservesIntent_withConflict() {
            // Store original values
            int originalALength = 5;
            int originalBLength = 5;

            // Create fresh operations for each transform
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(7, 5, "client2", "op2");

            // Transform A against B (mutates opA)
            DeleteOp aPrime = (DeleteOp) OtTransform.transform(opA, opB);

            // Create FRESH operations for the second transform
            DeleteOp opA2 = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB2 = OperationFixtures.createDelete(7, 5, "client2", "op2");

            // Transform B against original A (not the mutated one)
            DeleteOp bPrime = (DeleteOp) OtTransform.transform(opB2, opA2);

            // Both should reduce their length due to overlap
            assertTrue(aPrime.length < originalALength,
                    "A should be shortened by overlap (was 5, now " + aPrime.length + ")");
            assertTrue(bPrime.length < originalBLength,
                    "B should be shortened by overlap (was 5, now " + bPrime.length + ")");

            // Verify the specific transformed values
            assertEquals(2, aPrime.length, "A's overlap is 3, so 5-3=2");
            assertEquals(2, bPrime.length, "B's overlap is 3, so 5-3=2");
        }

        @Test
        @DisplayName("Commutative property: order doesn't affect final regions")
        void commutativeProperty() {
            // Document state shouldn't depend on application order
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(7, 5, "client2", "op2");

            // Path 1: A then B'
            DeleteOp bPrime = (DeleteOp) OtTransform.transform(opB, opA);

            // Path 2: B then A'
            DeleteOp aPrime = (DeleteOp) OtTransform.transform(opA, opB);

            // After applying both paths, the total deleted region should be the same
            // A: [5,10), B: [7,12) → union should be [5,12)
            // A: [5,10), B': transformed against A
            // B: [7,12), A': transformed against B

            // The non-overlapping parts should be preserved
            int totalDeletedPath1 = opA.length + bPrime.length;
            int totalDeletedPath2 = opB.length + aPrime.length;

            assertEquals(totalDeletedPath1, totalDeletedPath2,
                    "Total deleted length should be same regardless of order");
        }

        @Test
        @DisplayName("Non-overlapping deletes are independent")
        void nonOverlapping_independent() {
            // Store original values
            int originalAPos = 0;
            int originalALength = 5;
            int originalBPos = 10;
            int originalBLength = 5;

            DeleteOp opA = OperationFixtures.createDelete(originalAPos, originalALength, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(originalBPos, originalBLength, "client2", "op2");

            DeleteOp aPrime = (DeleteOp) OtTransform.transform(opA, opB);
            DeleteOp bPrime = (DeleteOp) OtTransform.transform(opB, opA);

            // A should be unchanged (B is after)
            assertEquals(originalAPos, aPrime.pos,
                    "A position unchanged when B is after");
            assertEquals(originalALength, aPrime.length,
                    "A length unchanged when B is after");

            // B should shift left by A's length
            assertEquals(originalBPos - originalALength, bPrime.pos, "B should shift left by A's length (10 - 5 = 5)");
            assertEquals(originalBLength, bPrime.length, "B length unchanged when no overlap");
        }
    }

    @Nested
    @DisplayName("Boundary conditions")
    class BoundaryConditions {

        @Test
        @DisplayName("B ends exactly where A starts")
        void bEnds_exactlyWhereAStarts() {
            // B: [0, 5), A: [5, 10)
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            // b.pos + b.length = 5, a.pos = 5
            // This is the boundary case: b.pos + b.length <= a.pos
            assertEquals(0, transformed.pos, "Should shift to position 0");
            assertEquals(5, transformed.length, "No overlap, length unchanged");
        }

        @Test
        @DisplayName("A ends exactly where B starts")
        void aEnds_exactlyWhereBStarts() {
            // A: [0, 5), B: [5, 10)
            DeleteOp opA = OperationFixtures.createDelete(0, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(5, 5, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            // b.pos = 5, a.pos + a.length = 5
            // This is the boundary case: b.pos >= a.pos + a.length
            assertEquals(0, transformed.pos, "Position unchanged");
            assertEquals(5, transformed.length, "No overlap, length unchanged");
        }

        @Test
        @DisplayName("One-off before boundary (just overlaps)")
        void oneOff_beforeBoundary_overlaps() {
            // B: [0, 6), A: [5, 10) → overlap [5, 6) = 1
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 6, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(0, transformed.pos);
            assertEquals(4, transformed.length, "Should reduce by 1");
        }

        @Test
        @DisplayName("One-off after boundary (no overlap)")
        void oneOff_afterBoundary_noOverlap() {
            // B: [0, 4), A: [5, 10) → no overlap
            DeleteOp opA = OperationFixtures.createDelete(5, 5, "client1", "op1");
            DeleteOp opB = OperationFixtures.createDelete(0, 4, "client2", "op2");

            DeleteOp transformed = (DeleteOp) OtTransform.transform(opA, opB);

            assertEquals(1, transformed.pos, "Should shift by B's length");
            assertEquals(5, transformed.length, "No overlap");
        }
    }
}