package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleDecision;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralFingerprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralSignals;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.NoveltyMetric;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ScheduleTrace;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestStatus;

class FeedbackScheduleCorpusTest {

    private static final ScheduleTrace MUTABLE_TRACE = new ScheduleTrace(List.of(
            new ScheduleDecision(3, 0),
            new ScheduleDecision(1, 0)));

    @Test
    void signalCorpusIgnoresRenamedStepsRepeatedEffectsAndDatabaseIds() {
        TestResult first = effectResult("write", 1, 1);
        TestResult incidentalChanges = effectResult("renamed", 3, 99);
        assertNotEquals(BehavioralFingerprint.from(first).hash(), BehavioralFingerprint.from(incidentalChanges).hash());
        assertEquals(BehavioralSignals.from(first), BehavioralSignals.from(incidentalChanges));
        FeedbackScheduleCorpus signals = new FeedbackScheduleCorpus(NoveltyMetric.BEHAVIORAL_SIGNALS);
        assertTrue(signals.observe(first).admittedToCorpus());
        var duplicate = signals.observe(incidentalChanges);
        assertFalse(duplicate.newBehavior());
        assertEquals(0, duplicate.newFeatures());
        assertFalse(duplicate.admittedToCorpus());
        assertEquals(1, duplicate.corpusSize());
        FeedbackScheduleCorpus detailedFingerprint = new FeedbackScheduleCorpus(NoveltyMetric.DETAILED_FINGERPRINT);
        detailedFingerprint.observe(first);
        assertTrue(detailedFingerprint.observe(incidentalChanges).admittedToCorpus());
    }

    @Test
    void signalCorpusKeepsNewOutcomeCombinationsAndRejectsIncompleteRuns() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus(NoveltyMetric.BEHAVIORAL_SIGNALS);
        var incomplete = corpus.observe(result(Set.of(
                TestStatus.EXECUTION_LIMIT_EXCEEDED, TestStatus.INTERNAL_SYSTEM_EXCEPTION)));
        assertFalse(incomplete.rewardEligible());
        assertFalse(incomplete.admittedToCorpus());
        assertEquals(1, corpus.observe(result(Set.of(TestStatus.INTERNAL_SYSTEM_EXCEPTION))).newFeatures());
        assertEquals(1, corpus.observe(result(Set.of(TestStatus.CRITICAL_STEP_FAILURE))).newFeatures());
        var combination = corpus.observe(result(Set.of(
                TestStatus.INTERNAL_SYSTEM_EXCEPTION, TestStatus.CRITICAL_STEP_FAILURE)));
        assertTrue(combination.newBehavior());
        assertEquals(0, combination.newFeatures());
        assertTrue(combination.admittedToCorpus());
    }

    @Test
    void signalGuidanceIsSeededAndKeepsDetailedParentIdentity() {
        TestResult first = effectResult("write", 1, 1);
        FeedbackScheduleCorpus left = new FeedbackScheduleCorpus(NoveltyMetric.BEHAVIORAL_SIGNALS);
        FeedbackScheduleCorpus right = new FeedbackScheduleCorpus(NoveltyMetric.BEHAVIORAL_SIGNALS);
        left.observe(first);
        right.observe(first);
        Random leftRandom = new Random(42L);
        Random rightRandom = new Random(42L);
        boolean sawGuidance = false;
        for (int run = 0; run < 20; run++) {
            // Identical seeds and corpus state should produce identical plans.
            var plan = left.nextPlan(leftRandom);
            assertEquals(plan, right.nextPlan(rightRandom));
            // A parent hash identifies a corpus-guided plan rather than a random schedule.
            if (plan.parentFingerprintHash() != null) {
                sawGuidance = true;
                // Parent identity stays detailed; this plan must mutate at least one choice.
                assertEquals(BehavioralFingerprint.from(first).hash(), plan.parentFingerprintHash());
                assertTrue(plan.mutatedChoices() > 0);
                assertNotEquals(0, plan.choicePrefix().getFirst());
            }
        }
        assertTrue(sawGuidance);
    }

    private static TestResult effectResult(String stepName, int repeats, int aggregateId) {
        StepId writer = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("writer"), stepName);
        var effects = IntStream.range(0, repeats)
                .mapToObj(index -> new StepEffect(index, writer, StepKind.FUNCTIONALITY,
                        StepEffect.EffectKind.WRITE, aggregateId, "Quiz")).toList();
        return new TestResult(new StepDependencies(), new StepDependencies(), Map.of(), List.of(writer), MUTABLE_TRACE,
                Map.of(), Set.of(), effects, Set.of(), List.of(), List.of(), Map.of());
    }

    @Test
    void admitsNovelFeatureCombinationsEvenWhenTheyAddNoAtomicFeature() {
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus(NoveltyMetric.DETAILED_FINGERPRINT);

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
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus(NoveltyMetric.DETAILED_FINGERPRINT);

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
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus(NoveltyMetric.DETAILED_FINGERPRINT);

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
        FeedbackScheduleCorpus corpus = new FeedbackScheduleCorpus(NoveltyMetric.DETAILED_FINGERPRINT);
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
