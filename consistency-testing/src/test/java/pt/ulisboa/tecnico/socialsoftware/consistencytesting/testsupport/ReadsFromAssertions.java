package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testsupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/**
 * Assertion helpers for the {@link ReadsFromRelation}s captured on a
 * {@link TestResult}.
 */
public final class ReadsFromAssertions {

    private ReadsFromAssertions() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean containsRelation(
            TestResult result, StepId reader, StepId writer, String aggregateType) {

        return result.readsFromRelations().contains(new ReadsFromRelation(reader, writer, aggregateType));
    }

    public static void assertReadsFrom(
            TestResult result, StepId reader, StepId writer, String aggregateType) {

        assertTrue(containsRelation(result, reader, writer, aggregateType),
                "Expected reads-from relation reader=[%s] writer=[%s] aggregateType=[%s], but it was absent. Captured relations: %s"
                        .formatted(reader, writer, aggregateType, result.readsFromRelations()));
    }

    public static void assertDoesNotReadFrom(
            TestResult result, StepId reader, StepId writer, String aggregateType) {

        assertFalse(containsRelation(result, reader, writer, aggregateType),
                "Did not expect reads-from relation reader=[%s] writer=[%s] aggregateType=[%s], but it was present"
                        .formatted(reader, writer, aggregateType));
    }

    /** All relations whose value was observed by {@code reader}. */
    public static Set<ReadsFromRelation> relationsByReader(TestResult result, StepId reader) {
        return result.readsFromRelations().stream()
                .filter(relation -> relation.reader().equals(reader))
                .collect(Collectors.toSet());
    }

    public static Set<ReadsFromRelation> relationsForAggregateType(TestResult result, String aggregateType) {
        return result.readsFromRelations().stream()
                .filter(relation -> relation.aggregateType().equals(aggregateType))
                .collect(Collectors.toSet());
    }

    /** Asserts that every captured relation was written by {@code writer}. */
    public static void assertAllRelationsWrittenBy(TestResult result, StepId writer) {
        Set<ReadsFromRelation> offending = result.readsFromRelations().stream()
                .filter(relation -> !relation.writer().equals(writer))
                .collect(Collectors.toSet());

        assertTrue(offending.isEmpty(),
                "Expected every relation to be written by [%s], but these were not: %s"
                        .formatted(writer, offending));
    }

    /**
     * Asserts no step reads-from itself. A step that reads an aggregate it wrote
     * earlier in the same step is, by construction, not a cross-step reads-from
     * and must never be recorded as a relation.
     */
    public static void assertNoSelfReadsFrom(TestResult result) {
        Set<ReadsFromRelation> selfRelations = result.readsFromRelations().stream()
                .filter(relation -> relation.reader().equals(relation.writer()))
                .collect(Collectors.toSet());

        assertEquals(Set.of(), selfRelations, "No step should read-from itself, but found: " + selfRelations);
    }
}
