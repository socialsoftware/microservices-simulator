package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Anomaly;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

class CampaignMetricsTest {

    @Test
    void separatesRawAnomaliesInvariantBreaksExceptionsAndStatuses() {
        CampaignMetrics metrics = new CampaignMetrics(1_000L);
        metrics.record(cleanResult(), 1_005L);
        metrics.record(resultWithAllOutcomeSignals(), 1_025L);

        OrchestrationReport.OutcomeMetrics snapshot = metrics.snapshot(10);

        assertEquals(10, snapshot.runsPlanned());
        assertEquals(2, snapshot.runsCompleted());
        assertEquals(1, snapshot.anomaliesObserved());
        assertEquals(1, snapshot.runsWithAnomalies());
        assertEquals(25L, snapshot.firstAnomalyElapsedMillis());
        assertEquals(3, snapshot.interInvariantViolationsObserved());
        assertEquals(1, snapshot.runsWithInterInvariantViolations());
        assertEquals(25L, snapshot.firstInterInvariantViolationElapsedMillis());
        assertEquals(List.of("courseHasQuestions", "quizHasQuestions"), snapshot.violatedInterInvariantNames());
        assertEquals(1, snapshot.stepExceptionsObserved());
        assertEquals(1, snapshot.runsWithStepExceptions());
        assertEquals(Map.of("CRITICAL_STEP_FAILURE", 1, "ISOLATION_ANOMALY", 1), snapshot.statusRunCounts());
    }

    @Test
    void leavesFirstObservationTimesAbsentWhenNoSignalWasObserved() {
        CampaignMetrics metrics = new CampaignMetrics(1_000L);
        metrics.record(cleanResult(), 1_005L);

        OrchestrationReport.OutcomeMetrics snapshot = metrics.snapshot(1);

        assertNull(snapshot.firstAnomalyElapsedMillis());
        assertNull(snapshot.firstInterInvariantViolationElapsedMillis());
    }

    private static TestResult cleanResult() {
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(), Map.of(), Set.of(),
                List.of(), Set.of(), List.of(), List.of(), Map.of());
    }

    private static TestResult resultWithAllOutcomeSignals() {
        FunctionalityId functionality = FunctionalityId.forSagaFunctionality("createQuiz");
        StepId step = StepId.forFunctionalityStep(functionality, "saveQuiz");
        Anomaly anomaly = new Anomaly.WriteSkew(
                functionality, FunctionalityId.forSagaFunctionality("addQuestion"), "Quiz", "Question");

        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(),
                Map.of(step, new IllegalStateException("failed step")),
                Set.of(TestStatus.CRITICAL_STEP_FAILURE, TestStatus.ISOLATION_ANOMALY),
                List.of(), Set.of(), List.of(), List.of(anomaly),
                Map.of(
                        "quizHasQuestions", Set.of(new InterInvariantViolation("quiz has no questions")),
                        "courseHasQuestions", Set.of(
                                new InterInvariantViolation("course has no questions"),
                                new InterInvariantViolation("course has two quizzes"))));
    }
}
