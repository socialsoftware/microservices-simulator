package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromTarget;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

class ReadsFromGuidanceTest {
    private final StepId writer = StepId.forFunctionalityStep(
            FunctionalityId.forSagaFunctionality("writer"), "write");
    private final StepId reader = StepId.forFunctionalityStep(
            FunctionalityId.forSagaFunctionality("reader"), "read");

    @Test
    void proposesRelationOnlyWhenStepsTouchedSameObject() {
        ReadsFromGuidance guidance = new ReadsFromGuidance();
        Random random = new Random(42L);
        assertNull(guidance.nextTarget(random)); // No run has supplied a candidate yet.

        guidance.observe(result(1, 2));
        assertNull(guidance.nextTarget(random)); // Different objects: no candidate relation.

        guidance.observe(result(1, 1));
        assertEquals(new ReadsFromTarget(writer, reader, "Quiz"), guidance.nextTarget(random));
    }

    @Test
    void doesNotTargetOrderingInsideOneSaga() {
        // Both steps touch the same object, but belong to the same saga. The scheduler
        // cannot change their intra-saga order, so no cross-saga target is proposed.
        StepId earlier = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality("writer"), "earlierRead");
        List<StepEffect> effects = List.of(
                new StepEffect(0, earlier, StepKind.FUNCTIONALITY,
                        StepEffect.EffectKind.READ, 1, "Quiz"),
                new StepEffect(1, writer, StepKind.FUNCTIONALITY,
                        StepEffect.EffectKind.WRITE, 1, "Quiz"));
        TestResult result = new TestResult(new StepDependencies(), new StepDependencies(), Map.of(),
                List.of(earlier, writer), Map.of(), Set.of(), effects,
                ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of());
        ReadsFromGuidance guidance = new ReadsFromGuidance();
        guidance.observe(result);
        assertNull(guidance.nextTarget(new Random(42L)));
    }

    @Test
    void stopsRetryingNeverObservedRelationAfterThreeAttempts() {
        ReadsFromGuidance guidance = new ReadsFromGuidance();
        guidance.observe(result(1, 1));
        Random guided = new Random() {
            @Override
            public double nextDouble() { // Always take the guided branch (not random exploration).
                return 1.0;
            }

            @Override
            public int nextInt(int bound) {
                return 0; // Always choose the first target deterministically.
            }
        };
        for (int attempt = 0; attempt < 3; attempt++) {
            assertEquals(new ReadsFromTarget(writer, reader, "Quiz"), guidance.nextTarget(guided));
        }
        assertNull(guidance.nextTarget(guided)); // Stops after 3 attempts without a successful read-from.
    }

    private TestResult result(int writtenId, int readId) {
        List<StepEffect> effects = List.of(
                new StepEffect(0, reader, StepKind.FUNCTIONALITY, StepEffect.EffectKind.READ, readId, "Quiz"),
                new StepEffect(1, writer, StepKind.FUNCTIONALITY, StepEffect.EffectKind.WRITE, writtenId, "Quiz"));
        return new TestResult(new StepDependencies(), new StepDependencies(), Map.of(),
                List.of(reader, writer), Map.of(), Set.of(), effects,
                ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of());
    }
}
