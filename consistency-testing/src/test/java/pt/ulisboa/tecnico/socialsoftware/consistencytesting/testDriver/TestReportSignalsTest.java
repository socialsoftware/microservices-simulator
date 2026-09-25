package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralFingerprint;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralSignals;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.Oracle;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepDependencies;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepKind;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

class TestReportSignalsTest {

    @Test
    void writesDetailedFingerprintAndBoundedSignalsWithoutChangingExplorerFeedback() {
        StepId writer = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("writer"), "write");
        StepId reader = StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality("reader"), "read");
        StepId commit = StepId.forCommitStep(FunctionalityId.forSagaFunctionality("writer"));
        List<StepEffect> effects = List.of(
                new StepEffect(0, writer, StepKind.FUNCTIONALITY, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                new StepEffect(1, reader, StepKind.FUNCTIONALITY, StepEffect.EffectKind.READ, 1, "Quiz"));
        TestResult result = new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(writer, reader, commit),
                Map.of(), Set.of(), effects, ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of());

        TestReport report = TestReport.from(new Oracle.TimedRun(result, 0L, 0L, 0L),
                ScheduleExplorationStrategy.UNIFORM_RANDOM,
                FeedbackScheduleCorpus.Plan.randomSchedule(),
                new FeedbackScheduleCorpus.Observation(true, false, 0, false, 0), 42L, 0L);

        assertEquals(BehavioralFingerprint.from(result).hash(), report.behavioralFingerprint().hash());
        assertEquals(BehavioralSignals.SCHEMA, report.behavioralSignals().schema());
        assertTrue(report.behavioralSignals().signals().contains(
                "uncommitted-read-exposure|writer|reader|Quiz|COMMITTED"));
        assertEquals(42L, report.schedulerSeed());
        assertEquals("uniform-random", report.scheduleExploration().strategy());
    }
}
