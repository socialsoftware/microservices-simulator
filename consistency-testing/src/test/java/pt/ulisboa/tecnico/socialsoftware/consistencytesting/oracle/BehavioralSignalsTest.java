package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

class BehavioralSignalsTest {

    @Test
    void stepNamesAndRepeatedEffectsDoNotCreateNewSignals() {
        StepId firstStep = step("writer", "first");
        StepId renamedStep = step("writer", "renamed");

        BehavioralSignals once = BehavioralSignals.from(result(
                List.of(firstStep),
                List.of(effect(0, firstStep, StepEffect.EffectKind.WRITE, 1, "Quiz"))));
        BehavioralSignals repeatedAndRenamed = BehavioralSignals.from(result(
                List.of(renamedStep),
                List.of(
                        effect(0, renamedStep, StepEffect.EffectKind.WRITE, 17, "Quiz"),
                        effect(1, renamedStep, StepEffect.EffectKind.WRITE, 17, "Quiz"))));

        assertEquals(once, repeatedAndRenamed);
        assertFalse(String.join("\n", once.signals()).contains("count="));
        assertFalse(String.join("\n", once.signals()).contains("first"));
    }

    @Test
    void conflictingAccessOrderRemainsDistinct() {
        StepId writer = step("writer", "write");
        StepId reader = step("reader", "read");

        BehavioralSignals writeBeforeRead = BehavioralSignals.from(result(
                List.of(writer, reader),
                List.of(effect(0, writer, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                        effect(1, reader, StepEffect.EffectKind.READ, 1, "Quiz"))));
        BehavioralSignals readBeforeWrite = BehavioralSignals.from(result(
                List.of(reader, writer),
                List.of(effect(0, reader, StepEffect.EffectKind.READ, 7, "Quiz"),
                        effect(1, writer, StepEffect.EffectKind.WRITE, 7, "Quiz"))));

        assertNotEquals(writeBeforeRead.hash(), readBeforeWrite.hash());
        assertTrue(writeBeforeRead.signals().contains("interaction|writer|WRITE|before|reader|READ|Quiz"));
        assertTrue(readBeforeWrite.signals().contains("interaction|reader|READ|before|writer|WRITE|Quiz"));
    }

    @Test
    void independentEffectsIgnoreOrderAndConcreteAggregateIds() {
        StepId first = step("first", "write");
        StepId second = step("second", "write");

        BehavioralSignals firstThenSecond = BehavioralSignals.from(result(
                List.of(first, second),
                List.of(effect(0, first, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                        effect(1, second, StepEffect.EffectKind.WRITE, 2, "Tournament"))));
        BehavioralSignals secondThenFirst = BehavioralSignals.from(result(
                List.of(second, first),
                List.of(effect(0, second, StepEffect.EffectKind.WRITE, 200, "Tournament"),
                        effect(1, first, StepEffect.EffectKind.WRITE, 100, "Quiz"))));

        assertEquals(firstThenSecond, secondThenFirst);
    }

    @Test
    void distinctReadsFromActorsRemainDistinct() {
        StepId firstWriter = step("first-writer", "write");
        StepId secondWriter = step("second-writer", "write");
        StepId reader = step("reader", "read");

        BehavioralSignals first = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(firstWriter, reader),
                Map.of(), Set.of(), List.of(),
                Set.of(new ReadsFromRelation(reader, firstWriter, "Quiz")), List.of(), List.of(), Map.of()));
        BehavioralSignals second = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(secondWriter, reader),
                Map.of(), Set.of(), List.of(),
                Set.of(new ReadsFromRelation(reader, secondWriter, "Quiz")), List.of(), List.of(), Map.of()));

        assertNotEquals(first, second);
        assertTrue(first.signals().contains("reads-from|first-writer|reader|Quiz"));
        assertTrue(second.signals().contains("reads-from|second-writer|reader|Quiz"));
    }

    @Test
    void semanticLockOutcomeRemainsDistinctWithoutAggregateId() {
        StepId step = step("update", "lock");
        SemanticLockId lock = new SemanticLockId("example.QuizState", "UPDATING");
        BehavioralSignals acquired = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(step),
                Map.of(), Set.of(), List.of(), Set.of(),
                List.of(new SemanticLockActivity(step, lock, 1, SemanticLockActivity.Outcome.ACQUIRED)),
                List.of(), Map.of()));
        BehavioralSignals rejected = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(step),
                Map.of(), Set.of(), List.of(), Set.of(),
                List.of(new SemanticLockActivity(step, lock, 99, SemanticLockActivity.Outcome.REJECTED)),
                List.of(), Map.of()));

        assertNotEquals(acquired, rejected);
        assertTrue(rejected.signals().contains("semantic-lock|update|example.QuizState#UPDATING|REJECTED"));
        assertFalse(String.join("\n", rejected.signals()).contains("99"));
    }

    @Test
    void eventCaptureLocationDoesNotCreateNewActorButHandlerPathRemainsVisible() {
        StepId firstEvent = eventStep(1, step("publisher", "first"));
        StepId secondEvent = eventStep(99, step("publisher", "second"));

        BehavioralSignals first = BehavioralSignals.from(result(
                List.of(firstEvent),
                List.of(effect(0, firstEvent, StepEffect.EffectKind.READ, 1, "Quiz"))));
        BehavioralSignals second = BehavioralSignals.from(result(
                List.of(secondEvent),
                List.of(effect(0, secondEvent, StepEffect.EffectKind.READ, 99, "Quiz"))));

        assertEquals(first, second);
        assertTrue(first.signals().stream().anyMatch(signal -> signal.startsWith("path|event-")));
        assertFalse(String.join("\n", first.signals()).contains("capturedAfter"));
    }

    @Test
    void compensationCommitAbortAndOutcomeFamiliesRemainVisible() {
        FunctionalityId functionality = FunctionalityId.forSagaFunctionality("update");
        StepId compensation = StepId.forCompensationStep(functionality, "write");
        StepId commit = StepId.forCommitStep(functionality);
        StepId abort = StepId.forAbortStep(functionality, "write");
        TestResult result = new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(),
                List.of(compensation, commit, abort),
                Map.of(abort, new IllegalStateException("id 17 failed")),
                Set.of(TestStatus.CRITICAL_STEP_FAILURE), List.of(), Set.of(), List.of(), List.of(),
                Map.of("OWNER_UNIQUE", Set.of(new InterInvariantViolation("broken"))));

        BehavioralSignals signals = BehavioralSignals.from(result);

        assertTrue(signals.signals().contains("path|update|COMPENSATION"));
        assertTrue(signals.signals().contains("path|update|COMMIT"));
        assertTrue(signals.signals().contains("path|update|ABORT"));
        assertTrue(signals.signals().contains("status|CRITICAL_STEP_FAILURE"));
        assertTrue(signals.signals().contains("inter-invariant-violation|OWNER_UNIQUE"));
        assertTrue(signals.signals().contains("exception|update|java.lang.IllegalStateException"));
    }

    @Test
    void foreignReadBeforeWriterCommitRecordsExposureWithoutCallingItAnAnomaly() {
        StepId writer = step("writer", "write");
        StepId reader = step("reader", "read");
        StepId commit = StepId.forCommitStep(FunctionalityId.forSagaFunctionality("writer"));
        List<StepEffect> effects = List.of(
                effect(0, writer, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                effect(1, reader, StepEffect.EffectKind.READ, 1, "Quiz"));

        BehavioralSignals signals = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(writer, reader, commit),
                Map.of(), Set.of(), effects, ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of()));

        assertTrue(signals.signals().contains("uncommitted-read-exposure|writer|reader|Quiz|COMMITTED"));
        assertFalse(signals.signals().stream().anyMatch(signal -> signal.startsWith("anomaly|")));
    }

    @Test
    void readAfterWriterCommitIsNotAnUncommittedExposure() {
        StepId writer = step("writer", "write");
        StepId commit = StepId.forCommitStep(FunctionalityId.forSagaFunctionality("writer"));
        StepId reader = step("reader", "read");
        List<StepEffect> effects = List.of(
                effect(0, writer, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                effect(1, reader, StepEffect.EffectKind.READ, 1, "Quiz"));

        BehavioralSignals signals = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(writer, commit, reader),
                Map.of(), Set.of(), effects, ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of()));

        assertFalse(signals.signals().stream().anyMatch(signal -> signal.startsWith("uncommitted-read-exposure|")));
    }

    @Test
    void readsFromInitialStateAreNotUncommittedExposures() {
        StepId reader = step("reader", "read");
        BehavioralSignals signals = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(reader),
                Map.of(), Set.of(), List.of(),
                Set.of(new ReadsFromRelation(reader, StepId.forInitialStateSetupStep(), "Quiz")),
                List.of(), List.of(), Map.of()));

        assertFalse(signals.signals().stream().anyMatch(signal -> signal.startsWith("uncommitted-read-exposure|")));
        assertTrue(signals.signals().contains("reads-from|initialStateSetup|reader|Quiz"));
    }

    @Test
    void exposureRetainsWhetherWriterLaterCompensated() {
        StepId writer = step("writer", "write");
        StepId reader = step("reader", "read");
        StepId compensation = StepId.forCompensationStep(FunctionalityId.forSagaFunctionality("writer"), "write");
        List<StepEffect> effects = List.of(
                effect(0, writer, StepEffect.EffectKind.WRITE, 1, "Quiz"),
                effect(1, reader, StepEffect.EffectKind.READ, 1, "Quiz"));

        BehavioralSignals signals = BehavioralSignals.from(new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), List.of(writer, reader, compensation),
                Map.of(), Set.of(), effects, ReadsFromRelation.deriveAll(effects), List.of(), List.of(), Map.of()));

        assertTrue(signals.signals().contains("uncommitted-read-exposure|writer|reader|Quiz|COMPENSATED"));
    }

    private static StepEffect effect(
            int sequence, StepId step, StepEffect.EffectKind kind, int aggregateId, String aggregateType) {
        return new StepEffect(sequence, step, step.stepKind(), kind, aggregateId, aggregateType);
    }

    private static StepId step(String functionality, String name) {
        return StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality(functionality), name);
    }

    private static StepId eventStep(int eventId, StepId capturedAfter) {
        TestEvent event = new TestEvent();
        event.setId(eventId);
        event.setPublisherAggregateId(eventId + 100);
        DeferredEventInvocation invocation = new DeferredEventInvocation(
                event, new TestEventHandler(), eventId + 200, () -> {
                });
        return StepId.forEventHandlerStep(
                FunctionalityId.forEventHandlerFunctionality(invocation, capturedAfter));
    }

    private static TestResult result(List<StepId> schedule, List<StepEffect> effects) {
        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), schedule,
                Map.of(), Set.of(), effects, Set.of(), List.of(), List.of(), Map.of());
    }

    private static final class TestEvent extends Event {
    }

    private static final class TestEventHandler extends EventHandler {
        private TestEventHandler() {
            super(null);
        }

        @Override
        public void handleEvent(Integer subscriberAggregateId, Event event) {
            // Identity-only test: no delivery is executed.
        }
    }
}
