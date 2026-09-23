package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleDecision;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleTrace;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

class FeedbackScheduleCorpusTest {

    private static final ScheduleTrace MUTABLE_TRACE = new ScheduleTrace(List.of(
            new ScheduleDecision(3, 0),
            new ScheduleDecision(1, 0)));

    @Test
    void admitsNovelFeatureCombinationsEvenWhenTheyAddNoAtomicFeature() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus();

        assertEquals(1, corpus.observe(result(Set.of(TestStatus.INTERNAL_SYSTEM_EXCEPTION))).newFeatures());
        assertEquals(1, corpus.observe(result(Set.of(TestStatus.CRITICAL_STEP_FAILURE))).newFeatures());

        FeedbackScheduleCorpus.Observation recombination = corpus.observe(result(Set.of(
                TestStatus.INTERNAL_SYSTEM_EXCEPTION,
                TestStatus.CRITICAL_STEP_FAILURE)));

        assertTrue(recombination.newBehavior());
        assertEquals(0, recombination.newFeatures());
        assertTrue(recombination.admittedToCorpus());
        assertEquals(3, recombination.corpusSize());
    }

    @Test
    void dependencyResolutionFailuresNeitherRewardNorPoisonFutureNovelty() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus();

        FeedbackScheduleCorpus.Observation failed = corpus.observe(result(Set.of(
                TestStatus.INTERDEPENDENCY_RESOLUTION_FAILED,
                TestStatus.INTERNAL_SYSTEM_EXCEPTION)));
        FeedbackScheduleCorpus.Observation laterValid = corpus.observe(
                result(Set.of(TestStatus.INTERNAL_SYSTEM_EXCEPTION)));

        assertFalse(failed.rewardEligible());
        assertFalse(failed.admittedToCorpus());
        assertTrue(laterValid.rewardEligible());
        assertTrue(laterValid.newBehavior());
        assertEquals(1, laterValid.newFeatures());
    }

    @Test
    void executionLimitFailuresNeitherRewardNorPoisonFutureNovelty() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus();

        FeedbackScheduleCorpus.Observation limited = corpus.observe(result(Set.of(
                TestStatus.EXECUTION_LIMIT_EXCEEDED,
                TestStatus.INTERNAL_SYSTEM_EXCEPTION)));
        FeedbackScheduleCorpus.Observation laterValid = corpus.observe(
                result(Set.of(TestStatus.INTERNAL_SYSTEM_EXCEPTION)));

        assertFalse(limited.rewardEligible());
        assertFalse(limited.admittedToCorpus());
        assertTrue(laterValid.rewardEligible());
        assertTrue(laterValid.newBehavior());
        assertEquals(1, laterValid.newFeatures());
    }

    @Test
    void guidedPlanMutatesAtLeastOneRunnableChoice() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus();
        corpus.observe(result(Set.of(TestStatus.INTERNAL_SYSTEM_EXCEPTION)));

        FeedbackScheduleCorpus.Plan plan = corpus.nextPlan(new Random(42L));

        assertEquals(1, plan.mutatedChoices());
        assertEquals(2, plan.choicePrefix().size());
        assertNotEquals(0, plan.choicePrefix().getFirst());
        assertEquals(0, plan.choicePrefix().get(1));
        assertTrue(plan.choicePrefix().getFirst() < 3);
    }

    private static TestResult result(Set<TestStatus> statuses) {
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(), MUTABLE_TRACE,
                Map.of(), statuses, List.of(), Set.of(), List.of(), List.of(), Map.of());
    }
}
