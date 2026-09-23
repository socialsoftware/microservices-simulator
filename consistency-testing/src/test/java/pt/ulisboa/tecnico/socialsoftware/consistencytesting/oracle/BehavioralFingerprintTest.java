package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Event;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.EventHandler;

class BehavioralFingerprintTest {

    private static final StepId PUBLISH = step("publish", "send");

    @Test
    void databaseGeneratedEventAndAggregateIdsDoNotCreateNovelBehavior() {
        StepId firstEvent = eventStep(17, 20, 30);
        StepId recreatedEvent = eventStep(901, 800, 700);

        TestResult first = result(
                List.of(PUBLISH, firstEvent),
                List.of(new StepEffect(0, firstEvent, StepKind.EVENT_HANDLER,
                        StepEffect.EffectKind.READ, 30, "Quiz")),
                Set.of(new ReadsFromRelation(firstEvent, StepId.forInitialStateSetupStep(), "Quiz")),
                List.of(new SemanticLockActivity(
                        firstEvent, new SemanticLockId("example.QuizState", "UPDATING"), 30,
                        SemanticLockActivity.Outcome.ACQUIRED)),
                Map.of());
        TestResult recreated = result(
                List.of(PUBLISH, recreatedEvent),
                List.of(new StepEffect(0, recreatedEvent, StepKind.EVENT_HANDLER,
                        StepEffect.EffectKind.READ, 700, "Quiz")),
                Set.of(new ReadsFromRelation(recreatedEvent, StepId.forInitialStateSetupStep(), "Quiz")),
                List.of(new SemanticLockActivity(
                        recreatedEvent, new SemanticLockId("example.QuizState", "UPDATING"), 700,
                        SemanticLockActivity.Outcome.ACQUIRED)),
                Map.of());

        BehavioralFingerprint firstFingerprint = BehavioralFingerprint.from(first);
        BehavioralFingerprint recreatedFingerprint = BehavioralFingerprint.from(recreated);

        assertEquals(firstFingerprint, recreatedFingerprint);
        assertFalse(String.join("\n", firstFingerprint.features()).contains("eventId"));
        assertFalse(String.join("\n", firstFingerprint.features()).contains("fromAggregate"));
        assertFalse(String.join("\n", firstFingerprint.features()).contains("toAggregate"));
    }

    @Test
    void unrelatedCrossFunctionalityOrderDoesNotManufactureBehavioralNovelty() {
        StepId first = step("first", "write");
        StepId second = step("second", "read");

        BehavioralFingerprint firstBeforeSecond = BehavioralFingerprint.from(
                result(List.of(first, second), List.of(), Set.of(), List.of(), Map.of()));
        BehavioralFingerprint secondBeforeFirst = BehavioralFingerprint.from(
                result(List.of(second, first), List.of(), Set.of(), List.of(), Map.of()));

        assertEquals(firstBeforeSecond, secondBeforeFirst);
        assertFalse(firstBeforeSecond.features().stream()
                .anyMatch(feature -> feature.startsWith("order|") || feature.startsWith("schedule|")));
    }

    @Test
    void conflictingAggregateRelationshipChangesBehaviorWithoutExposingItsId() {
        StepId writer = step("writer", "write");
        StepId reader = step("reader", "read");
        List<StepId> schedule = List.of(writer, reader);

        TestResult conflicting = result(
                schedule,
                List.of(
                        new StepEffect(0, writer, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.WRITE, 41, "Quiz"),
                        new StepEffect(1, reader, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.READ, 41, "Quiz")),
                Set.of(new ReadsFromRelation(reader, writer, "Quiz")), List.of(), Map.of());
        TestResult independent = result(
                schedule,
                List.of(
                        new StepEffect(0, writer, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.WRITE, 410, "Quiz"),
                        new StepEffect(1, reader, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.READ, 420, "Quiz")),
                Set.of(new ReadsFromRelation(reader, StepId.forInitialStateSetupStep(), "Quiz")), List.of(), Map.of());

        BehavioralFingerprint conflictFingerprint = BehavioralFingerprint.from(conflicting);
        BehavioralFingerprint independentFingerprint = BehavioralFingerprint.from(independent);

        assertNotEquals(conflictFingerprint.hash(), independentFingerprint.hash());
        assertTrue(conflictFingerprint.features().stream().anyMatch(feature -> feature.startsWith("conflict|")));
        assertFalse(String.join("\n", conflictFingerprint.features()).contains("41"));
    }

    @Test
    void orderOfConflictingEffectsChangesReadsFromBehavior() {
        StepId writer = step("writer", "write");
        StepId reader = step("reader", "read");

        TestResult writeBeforeRead = result(
                List.of(writer, reader),
                List.of(
                        new StepEffect(0, writer, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.WRITE, 41, "Quiz"),
                        new StepEffect(1, reader, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.READ, 41, "Quiz")),
                Set.of(new ReadsFromRelation(reader, writer, "Quiz")), List.of(), Map.of());
        TestResult readBeforeWrite = result(
                List.of(reader, writer),
                List.of(
                        new StepEffect(0, reader, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.READ, 41, "Quiz"),
                        new StepEffect(1, writer, StepKind.FUNCTIONALITY,
                                StepEffect.EffectKind.WRITE, 41, "Quiz")),
                Set.of(new ReadsFromRelation(reader, StepId.forInitialStateSetupStep(), "Quiz")), List.of(), Map.of());

        assertNotEquals(
                BehavioralFingerprint.from(writeBeforeRead).hash(), BehavioralFingerprint.from(readBeforeWrite).hash());
    }

    @Test
    void exceptionMessagesAndTheirEmbeddedIdsDoNotCreateNovelBehavior() {
        StepId step = step("create", "load");
        TestResult first = result(
                List.of(step), List.of(), Set.of(), List.of(),
                Map.of(step, new IllegalStateException("Aggregate 17 does not exist")));
        TestResult second = result(
                List.of(step), List.of(), Set.of(), List.of(),
                Map.of(step, new IllegalStateException("Aggregate 901 does not exist")));

        assertEquals(BehavioralFingerprint.from(first), BehavioralFingerprint.from(second));
    }

    @Test
    void longNestedEventChainsProduceBoundedFingerprintFeatures() {
        List<StepId> chain = new ArrayList<>();
        StepId capturedAfter = PUBLISH;
        for (int depth = 0; depth < 100; depth++) {
            capturedAfter = eventStep(17 + depth, 20, 30, capturedAfter);
            chain.add(capturedAfter);
        }
        List<StepEffect> effects = IntStream.range(0, chain.size())
                .mapToObj(index -> new StepEffect(
                        index, chain.get(index), StepKind.EVENT_HANDLER,
                        StepEffect.EffectKind.WRITE, 30, "Quiz"))
                .toList();

        BehavioralFingerprint fingerprint = BehavioralFingerprint.from(
                result(chain, effects, Set.of(), List.of(), Map.of()));

        assertTrue(fingerprint.features().stream().mapToInt(String::length).max().orElseThrow() < 1_024);
        assertTrue(fingerprint.features().stream().mapToLong(String::length).sum() < 5_000_000L);
    }

    private static StepId step(String functionality, String name) {
        return StepId.forFunctionalityStep(FunctionalityId.forSagaFunctionality(functionality), name);
    }

    private static StepId eventStep(int eventId, int publisherId, int subscriberId) {
        return eventStep(eventId, publisherId, subscriberId, PUBLISH);
    }

    private static StepId eventStep(
            int eventId, int publisherId, int subscriberId, StepId capturedAfter) {
        TestEvent event = new TestEvent();
        event.setId(eventId);
        event.setPublisherAggregateId(publisherId);
        DeferredEventInvocation invocation = new DeferredEventInvocation(
                event, new TestEventHandler(), subscriberId, () -> {
                });
        return StepId.forEventHandlerStep(
                FunctionalityId.forEventHandlerFunctionality(invocation, capturedAfter));
    }

    private static TestResult result(
            List<StepId> schedule,
            List<StepEffect> effects,
            Set<ReadsFromRelation> readsFrom,
            List<SemanticLockActivity> locks,
            Map<StepId, Exception> exceptions) {

        return new TestResult(
                new StepDependencies(), new StepDependencies(), Map.of(), schedule,
                exceptions, Set.of(), effects, readsFrom, locks, List.of(), Map.of());
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
