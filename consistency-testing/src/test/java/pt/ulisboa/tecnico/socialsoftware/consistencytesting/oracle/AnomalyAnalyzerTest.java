package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect.EffectKind;

/**
 * Pure unit tests for {@link AnomalyAnalyzer} over hand-built synthetic effect
 * sequences — no Spring context, no application under test.
 * <p>
 * Besides one positive case per anomaly, there is one negative case per
 * documented rule constraint: these tests pin the constraints in place (the
 * constraints themselves are documented on the analyzer's {@code detect*}
 * methods, alongside their enforcement).
 */
class AnomalyAnalyzerTest {

    private static final String TOURNAMENT = "Tournament";
    private static final String TOPIC = "Topic";

    private static final FunctionalityId FUNC_A = FunctionalityId.forSagaFunctionality("funcA");
    private static final FunctionalityId FUNC_B = FunctionalityId.forSagaFunctionality("funcB");

    /**
     * Stands in for an event-handler functionality (a subscriber reacting to an
     * event). The analyzer only cares about a step's {@link StepKind} and the
     * identity of its {@link FunctionalityId}, so a plain id paired with
     * {@link StepKind#EVENT_HANDLER} effects faithfully models a handler here.
     */
    private static final FunctionalityId FUNC_H = FunctionalityId.forSagaFunctionality("funcH");

    private static final int AGGREGATE_X = 101;
    private static final int AGGREGATE_Y = 202;

    private List<StepEffect> effects;
    private int nextSequenceNumber;

    @BeforeEach
    void resetSequence() {
        effects = new ArrayList<>();
        nextSequenceNumber = 0;
    }

    private void read(FunctionalityId func, String stepName, StepKind stepKind, int aggregateId, String type) {
        effects.add(new StepEffect(nextSequenceNumber++,
                StepId.forFunctionalityStep(func, stepName), stepKind, EffectKind.READ, aggregateId, type));
    }

    private void write(FunctionalityId func, String stepName, StepKind stepKind, int aggregateId, String type) {
        effects.add(new StepEffect(nextSequenceNumber++,
                StepId.forFunctionalityStep(func, stepName), stepKind, EffectKind.WRITE, aggregateId, type));
    }

    private List<Anomaly> analyze(Set<FunctionalityId> compensated, Set<FunctionalityId> committed) {
        return AnomalyAnalyzer.analyze(effects, compensated, committed);
    }

    private static List<Anomaly> ofType(List<Anomaly> anomalies, AnomalyType type) {
        return anomalies.stream().filter(anomaly -> anomaly.type() == type).toList();
    }

    @Nested
    class DirtyRead {

        /** B reads A's write; A later compensates: the read was of doomed data. */
        @Test
        void foreignReadOfCompensatedWriteIsDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "readStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(FUNC_A), Set.of(FUNC_B)), AnomalyType.DIRTY_READ);

            assertEquals(1, found.size());
            Anomaly.DirtyRead dirtyRead = (Anomaly.DirtyRead) found.get(0);
            assertEquals(StepId.forFunctionalityStep(FUNC_B, "readStep"), dirtyRead.reader());
            assertEquals(StepId.forFunctionalityStep(FUNC_A, "writeStep"), dirtyRead.doomedWriter());
            assertEquals(TOURNAMENT, dirtyRead.aggregateType());
        }

        /**
         * An event handler is application logic too: a handler reacting to an
         * event by reading doomed data is just as much a dirty read as a saga
         * step doing it.
         */
        @Test
        void eventHandlerReadOfCompensatedWriteIsDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_H, "handlerStep", StepKind.EVENT_HANDLER, AGGREGATE_X, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(FUNC_A), Set.of(FUNC_H)), AnomalyType.DIRTY_READ);

            assertEquals(1, found.size());
            Anomaly.DirtyRead dirtyRead = (Anomaly.DirtyRead) found.get(0);
            assertEquals(StepId.forFunctionalityStep(FUNC_H, "handlerStep"), dirtyRead.reader());
            assertEquals(StepId.forFunctionalityStep(FUNC_A, "writeStep"), dirtyRead.doomedWriter());
            assertEquals(TOURNAMENT, dirtyRead.aggregateType());
        }

        /** Constraint: machinery (commit) reads are supposed to see fresh data. */
        @Test
        void commitMachineryReadOfCompensatedWriteIsNotDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "commitStep", StepKind.COMMIT, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(FUNC_A), Set.of(FUNC_B)), AnomalyType.DIRTY_READ).isEmpty());
        }

        /**
         * Constraint: a compensation read is a functionality's undo bookkeeping,
         * not a business observation, so it is not a victim of a dirty read.
         */
        @Test
        void compensationReadOfCompensatedWriteIsNotDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "undoReadStep", StepKind.COMPENSATION, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(FUNC_A), Set.of()), AnomalyType.DIRTY_READ).isEmpty());
        }

        /** Constraint: a compensation write restores state; reading it is not dirty. */
        @Test
        void readOfACompensationWriteIsNotDirty() {
            write(FUNC_A, "undoStep", StepKind.COMPENSATION, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "readStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(FUNC_A), Set.of(FUNC_B)), AnomalyType.DIRTY_READ).isEmpty());
        }

        /** Constraint: reading one's own doomed write is not an isolation anomaly. */
        @Test
        void readingOwnDoomedWriteIsNotDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "laterReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(FUNC_A), Set.of()), AnomalyType.DIRTY_READ).isEmpty());
        }

        /** Constraint: the writer must actually have entered the compensation path. */
        @Test
        void readOfANonCompensatedWriteIsNotDirty() {
            write(FUNC_A, "writeStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "readStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.DIRTY_READ).isEmpty());
        }
    }

    @Nested
    class NonRepeatableRead {

        /** A reads X, B overwrites X, A re-reads X and sees a different version. */
        @Test
        void foreignWriteBetweenTwoReadsIsNonRepeatable() {
            read(FUNC_A, "firstReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "interposedWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "secondReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)),
                    AnomalyType.NON_REPEATABLE_READ);

            assertEquals(1, found.size());
            Anomaly.NonRepeatableRead nonRepeatableRead = (Anomaly.NonRepeatableRead) found.get(0);
            assertEquals(FUNC_A, nonRepeatableRead.functionality());
            assertEquals(StepId.forFunctionalityStep(FUNC_A, "firstReadStep"), nonRepeatableRead.firstRead());
            assertEquals(StepId.forFunctionalityStep(FUNC_A, "secondReadStep"), nonRepeatableRead.secondRead());
            assertEquals(StepId.forInitialStateSetupStep(), nonRepeatableRead.firstWriter());
            assertEquals(StepId.forFunctionalityStep(FUNC_B, "interposedWriteStep"),
                    nonRepeatableRead.secondWriter());
            assertEquals(TOURNAMENT, nonRepeatableRead.aggregateType());
        }

        /**
         * The reader can be an event handler: a handler that reads the same
         * aggregate twice (e.g. once directly, once via a nested lookup) and sees
         * a foreign write in between suffers a non-repeatable read like any
         * functionality would.
         */
        @Test
        void eventHandlerReReadingAcrossForeignWriteIsNonRepeatable() {
            read(FUNC_H, "firstHandlerReadStep", StepKind.EVENT_HANDLER, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "interposedWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_H, "secondHandlerReadStep", StepKind.EVENT_HANDLER, AGGREGATE_X, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(), Set.of(FUNC_H, FUNC_B)),
                    AnomalyType.NON_REPEATABLE_READ);

            assertEquals(1, found.size());
            Anomaly.NonRepeatableRead nonRepeatableRead = (Anomaly.NonRepeatableRead) found.get(0);
            assertEquals(FUNC_H, nonRepeatableRead.functionality());
            assertEquals(StepId.forFunctionalityStep(FUNC_B, "interposedWriteStep"),
                    nonRepeatableRead.secondWriter());
        }

        /**
         * The interposing writer can be an event handler: a denormalization sync
         * performed by a handler between a functionality's two reads is a genuine
         * different version, so it is a non-repeatable read. This is the
         * writer-side counterpart to the app-level event-handler scenario.
         */
        @Test
        void foreignEventHandlerWriteBetweenTwoReadsIsNonRepeatable() {
            read(FUNC_A, "firstReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_H, "handlerWriteStep", StepKind.EVENT_HANDLER, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "secondReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_H)),
                    AnomalyType.NON_REPEATABLE_READ);

            assertEquals(1, found.size());
            Anomaly.NonRepeatableRead nonRepeatableRead = (Anomaly.NonRepeatableRead) found.get(0);
            assertEquals(FUNC_A, nonRepeatableRead.functionality());
            assertEquals(StepId.forFunctionalityStep(FUNC_H, "handlerWriteStep"),
                    nonRepeatableRead.secondWriter());
        }

        /** Two reads of the same unchanged version are repeatable. */
        @Test
        void twoReadsOfTheSameVersionAreRepeatable() {
            read(FUNC_A, "firstReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "secondReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A)), AnomalyType.NON_REPEATABLE_READ).isEmpty());
        }

        /**
         * Constraint: a functionality re-reading its OWN interposed write (made
         * by a different step of the same functionality) is expected behavior.
         */
        @Test
        void reReadingOwnWriteIsNotNonRepeatable() {
            read(FUNC_A, "firstReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_A, "ownWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "secondReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A)), AnomalyType.NON_REPEATABLE_READ).isEmpty());
        }

        /**
         * Constraint: commit-machinery re-reads intentionally fetch the freshest
         * version (the rebase mechanism) and must not count as the second read.
         */
        @Test
        void commitMachineryReReadIsNotNonRepeatable() {
            read(FUNC_A, "firstReadStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "interposedWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "commitStep", StepKind.COMMIT, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.NON_REPEATABLE_READ)
                    .isEmpty());
        }
    }

    @Nested
    class WriteSkew {

        /**
         * The classic skew: A and B each read the aggregate the other one later
         * writes, neither sees the other's write, both commit.
         */
        @Test
        void mutualBlindReadWriteOnDistinctAggregatesIsWriteSkew() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_B, "writeYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW);

            assertEquals(1, found.size());
            Anomaly.WriteSkew writeSkew = (Anomaly.WriteSkew) found.get(0);
            Set<FunctionalityId> pair = Set.of(writeSkew.functionalityA(), writeSkew.functionalityB());
            assertEquals(Set.of(FUNC_A, FUNC_B), pair);
        }

        /** Only one rw direction (a plain anti-dependency) is serializable. */
        @Test
        void singleDirectionAntiDependencyIsNotWriteSkew() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            write(FUNC_B, "writeYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW).isEmpty());
        }

        /**
         * Constraint: a wr edge between the pair means one saw the other's
         * write — partially serialized, not mutually blind.
         */
        @Test
        void pairConnectedByAWrEdgeIsNotWriteSkew() {
            // mutual rw exists (A read Y that B later writes; B read X that A later
            // writes) ...
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            // ... but B then READS the X version A wrote: wr edge, B decided knowing
            // A's write
            read(FUNC_B, "lateReadXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_B, "writeYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW).isEmpty());
        }

        /**
         * Constraint: mutual rw on one single shared aggregate is the
         * intra-aggregate conflict the oracle's rebase machinery serializes, not
         * a write skew.
         */
        @Test
        void mutualRwOnTheSameSingleAggregateIsNotWriteSkew() {
            read(FUNC_A, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW).isEmpty());
        }

        /**
         * Two distinct aggregate INSTANCES of the same type are a valid skew
         * (the requirement is on instances, not types) — this is exactly the
         * two-tournaments quota scenario.
         */
        @Test
        void distinctAggregatesOfTheSameTypeAreAWriteSkew() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "writeYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);

            assertEquals(1,
                    ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW).size());
        }

        /** Constraint: a compensated functionality never materialized its decision. */
        @Test
        void pairWhereOneFunctionalityDidNotCommitIsNotWriteSkew() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_B, "writeYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A)), AnomalyType.WRITE_SKEW).isEmpty());
        }

        /**
         * An event handler's write is an application decision, so it forms rw
         * edges and can be one side of a write skew: here a functionality and a
         * handler each read the aggregate the other later writes, both commit.
         */
        @Test
        void eventHandlerWriteParticipatesInWriteSkew() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_H, "handlerReadXStep", StepKind.EVENT_HANDLER, AGGREGATE_X, TOPIC);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_H, "handlerWriteYStep", StepKind.EVENT_HANDLER, AGGREGATE_Y, TOURNAMENT);

            List<Anomaly> found = ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_H)), AnomalyType.WRITE_SKEW);

            assertEquals(1, found.size());
            Anomaly.WriteSkew writeSkew = (Anomaly.WriteSkew) found.get(0);
            Set<FunctionalityId> pair = Set.of(writeSkew.functionalityA(), writeSkew.functionalityB());
            assertEquals(Set.of(FUNC_A, FUNC_H), pair);
        }

        /** Constraint: machinery effects are bookkeeping, they create no rw edges. */
        @Test
        void machineryWritesCreateNoRwEdges() {
            read(FUNC_A, "readYStep", StepKind.FUNCTIONALITY, AGGREGATE_Y, TOURNAMENT);
            read(FUNC_B, "readXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            write(FUNC_A, "writeXStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOPIC);
            // B's overwrite of Y happens in its commit machinery, not in an
            // application step: no rw edge towards it
            write(FUNC_B, "commitStep", StepKind.COMMIT, AGGREGATE_Y, TOURNAMENT);

            assertTrue(ofType(analyze(Set.of(), Set.of(FUNC_A, FUNC_B)), AnomalyType.WRITE_SKEW).isEmpty());
        }
    }

    @Nested
    class ReadsFromDerivation {

        /** Reads with no earlier writer resolve to the initial state setup step. */
        @Test
        void readWithoutWriterResolvesToInitialStateSetup() {
            read(FUNC_A, "readStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            Set<ReadsFromRelation> relations = ReadsFromRelation.deriveAll(effects);

            assertEquals(Set.of(new ReadsFromRelation(
                    StepId.forFunctionalityStep(FUNC_A, "readStep"),
                    StepId.forInitialStateSetupStep(),
                    TOURNAMENT)), relations);
        }

        /** The writer is the most recent earlier write of the same aggregate. */
        @Test
        void readResolvesToTheLatestEarlierWriter() {
            write(FUNC_A, "firstWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            write(FUNC_B, "secondWriteStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "readStep", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            Set<ReadsFromRelation> relations = ReadsFromRelation.deriveAll(effects);

            assertEquals(Set.of(new ReadsFromRelation(
                    StepId.forFunctionalityStep(FUNC_A, "readStep"),
                    StepId.forFunctionalityStep(FUNC_B, "secondWriteStep"),
                    TOURNAMENT)), relations);
        }

        /** A step reading what it itself wrote produces no cross-step relation. */
        @Test
        void selfReadProducesNoRelation() {
            write(FUNC_A, "step", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);
            read(FUNC_A, "step", StepKind.FUNCTIONALITY, AGGREGATE_X, TOURNAMENT);

            assertTrue(ReadsFromRelation.deriveAll(effects).isEmpty());
        }
    }
}
