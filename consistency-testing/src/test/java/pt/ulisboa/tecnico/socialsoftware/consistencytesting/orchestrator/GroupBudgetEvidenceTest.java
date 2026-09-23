package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleTrace;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockActivity;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.SemanticLockId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestDriver;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

class GroupBudgetEvidenceTest {

    @Test
    void countsOnlyMarginalBehaviorFeaturesAndFindingFamilies() {
        GroupBudgetEvidence evidence = new GroupBudgetEvidence();
        TestResult normal = result("step", null);
        TestResult firstFinding = result("step", "maximum-tournaments");

        AdaptiveGroupBudgetAllocator.BatchFeedback first = evidence.observe(
                List.of(normal, normal, firstFinding, firstFinding));
        AdaptiveGroupBudgetAllocator.BatchFeedback second = evidence.observe(
                List.of(result("other-step", "maximum-tournaments"),
                        result("step", "unique-owner")));

        assertEquals(2, first.newBehaviors());
        assertEquals(2, first.runsAddingFeatures());
        assertEquals(1, first.newFindingFamilies());
        assertEquals(2, second.newBehaviors());
        assertEquals(2, second.runsAddingFeatures());
        assertEquals(1, second.newFindingFamilies());
        assertEquals(2, evidence.uniqueFindingFamilies());
    }

    @Test
    void doesNotRewardInterdependencyResolutionFailures() {
        GroupBudgetEvidence evidence = new GroupBudgetEvidence();
        TestResult failed = result("failed", null, TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED);

        AdaptiveGroupBudgetAllocator.BatchFeedback feedback = evidence.observe(List.of(failed));

        assertEquals(0, feedback.newBehaviors());
        assertEquals(0, feedback.runsAddingFeatures());
        assertEquals(0, feedback.newFindingFamilies());
        assertEquals(0.0, feedback.reward());
    }

    @Test
    void doesNotRewardExecutionLimitFailures() {
        GroupBudgetEvidence evidence = new GroupBudgetEvidence();
        TestResult failed = result("failed", null, TestStatus.EXECUTION_LIMIT_EXCEEDED);

        AdaptiveGroupBudgetAllocator.BatchFeedback feedback = evidence.observe(List.of(failed));

        assertEquals(0, feedback.newBehaviors());
        assertEquals(0, feedback.runsAddingFeatures());
        assertEquals(0, feedback.newFindingFamilies());
        assertEquals(0.0, feedback.reward());
    }

    @Test
    void doesNotTreatAProtectedSemanticLockConflictAsAFindingOrReward() {
        GroupBudgetEvidence evidence = new GroupBudgetEvidence();
        TestResult protectedConflict = protectedSemanticLockConflict();

        AdaptiveGroupBudgetAllocator.BatchFeedback feedback = evidence.observe(List.of(protectedConflict));

        assertFalse(TestDriver.isFinding(protectedConflict));
        assertEquals(0, feedback.newBehaviors());
        assertEquals(0, feedback.runsAddingFeatures());
        assertEquals(0, feedback.newFindingFamilies());
        assertEquals(0.0, feedback.reward());
    }

    private static TestResult result(String stepName, String violatedInvariant) {
        return result(stepName, violatedInvariant, null);
    }

    private static TestResult result(
            String stepName, String violatedInvariant, TestStatus extraStatus) {

        StepId step = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality("first"), stepName);
        boolean finding = violatedInvariant != null;
        Set<TestStatus> statuses = new java.util.HashSet<>();
        if (finding) {
            statuses.add(TestStatus.INTER_INVARIANT_VIOLATION);
        }
        if (extraStatus != null) {
            statuses.add(extraStatus);
        }
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(step),
                ScheduleTrace.empty(), Map.of(),
                statuses,
                List.of(), Set.of(), List.of(), List.of(),
                finding
                        ? Map.of(violatedInvariant, Set.of(new InterInvariantViolation("broken")))
                        : Map.of());
    }

    private static TestResult protectedSemanticLockConflict() {
        StepId step = StepId.forFunctionalityStep(
                FunctionalityId.forSagaFunctionality("first"), "conflictingStep");
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(step), ScheduleTrace.empty(),
                Map.of(step, new SimulatorException("Aggregate is being used in %s saga.", "LOCKED")),
                Set.of(), List.of(), Set.of(),
                List.of(new SemanticLockActivity(step, SemanticLockId.from(TestSagaState.LOCKED), 1,
                        SemanticLockActivity.Outcome.REJECTED)),
                List.of(), Map.of());
    }

    private enum TestSagaState implements SagaState {
        LOCKED;

        @Override
        public String getStateName() {
            return "LOCKED";
        }
    }
}
