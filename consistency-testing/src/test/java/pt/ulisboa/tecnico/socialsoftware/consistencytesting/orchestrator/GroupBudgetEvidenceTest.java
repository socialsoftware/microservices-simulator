package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleTrace;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

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
}
