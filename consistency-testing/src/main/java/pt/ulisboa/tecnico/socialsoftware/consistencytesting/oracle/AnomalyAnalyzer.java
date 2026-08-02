package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.ReadsFromRelation.ResolvedRead;

/**
 * Detects isolation anomalies in a run, purely as a function of the run's
 * {@linkplain StepEffect effect sequence} (plus which functionalities
 * compensated and which committed).
 * <p>
 * Each rule's constraints are documented (and enforced) in the corresponding
 * {@code detect*} method: {@link #detectDirtyReads},
 * {@link #detectNonRepeatableReads}, {@link #detectWriteSkews}.
 *
 * <h2>Application effects vs machinery effects</h2>
 *
 * The rules distinguish effects that represent an <em>application decision or
 * observation</em> from effects that are the oracle's transaction
 * <em>machinery</em>. See {@link #isApplicationEffect}:
 * <ul>
 * <li>{@link StepKind#FUNCTIONALITY} and {@link StepKind#EVENT_HANDLER} are
 * application effects — a saga step, or a subscriber reacting to an event, both
 * read and write real aggregates to make real decisions, and either can be the
 * victim of an anomaly;</li>
 * <li>{@link StepKind#COMMIT} and {@link StepKind#ABORT} are machinery: the
 * commit path re-reads the freshest committed version and rebases onto it, the
 * abort path restores previous states. Their reads are deliberately fresh (so
 * counting them would flag every healed rebase) and their writes are the
 * committed/rebased result or state restoration, not a new decision;</li>
 * <li>{@link StepKind#COMPENSATION} is the undo action of a functionality:
 * a compensation <em>write</em> is the RESTORED (recovered) state, not doomed
 * data, and a compensation <em>read</em> is undo bookkeeping rather than a
 * business observation.</li>
 * </ul>
 * The <em>reader</em> side of every rule is restricted to application effects.
 * The <em>writer</em> side is restricted per rule by what that rule needs (see
 * each {@code detect*} method): dirty-read needs a doomed write, so only a
 * FUNCTIONALITY write qualifies; a non-repeatable read only needs the reader to
 * have observed a different foreign version, so its interposing writer is
 * intentionally kind-agnostic; write-skew's rw edges need an application write.
 */
final class AnomalyAnalyzer {

    private AnomalyAnalyzer() {
        throw new UnsupportedOperationException("Instantiation is not allowed");
    }

    /**
     * Whether {@code stepKind} is an application effect — a real read/write
     * decision made by a saga step ({@link StepKind#FUNCTIONALITY}) or by a
     * subscriber reacting to an event ({@link StepKind#EVENT_HANDLER}) — as
     * opposed to the oracle's transaction machinery ({@link StepKind#COMMIT},
     * {@link StepKind#ABORT}) or a functionality's undo
     * ({@link StepKind#COMPENSATION}).
     * <p>
     * Only application effects can be the <em>reader</em> victim of an anomaly.
     * Event handlers are included because a handler acting on doomed data, or
     * reading an aggregate a foreign write invalidated, is just as much a real
     * anomaly as a saga step doing the same.
     */
    private static boolean isApplicationEffect(StepKind stepKind) {
        return switch (stepKind) {
            case FUNCTIONALITY, EVENT_HANDLER -> true;
            case COMMIT, ABORT, COMPENSATION -> false;
        };
    }

    /**
     * Analyzes a finished run.
     *
     * @param effectSequence             every read/write effect of the run, in
     *                                   order of occurrence
     * @param compensatedFunctionalities functionalities that entered the
     *                                   compensation path (a compensation was
     *                                   injected for them), whether or not the
     *                                   compensation itself then succeeded
     * @param committedFunctionalities   functionalities whose commit step
     *                                   executed successfully
     * @return the anomalies found, in detection order
     */
    static List<Anomaly> analyze(
            List<StepEffect> effectSequence,
            Set<FunctionalityId> compensatedFunctionalities,
            Set<FunctionalityId> committedFunctionalities) {

        List<ResolvedRead> resolvedReads = ReadsFromRelation.resolveReads(effectSequence);

        List<Anomaly> anomalies = new ArrayList<>();
        anomalies.addAll(detectDirtyReads(resolvedReads, compensatedFunctionalities));
        anomalies.addAll(detectNonRepeatableReads(resolvedReads));
        anomalies.addAll(detectWriteSkews(effectSequence, resolvedReads, committedFunctionalities));
        return anomalies;
    }

    /**
     * DIRTY_READ: an application step (a saga functionality step or an event
     * handler) read a write made by a foreign functionality that later
     * compensated — the reader observed, and possibly acted on, doomed data.
     * <p>
     * Constraints, each enforced below:
     * <ul>
     * <li>the reader must be an {@linkplain #isApplicationEffect application}
     * effect — a FUNCTIONALITY or an EVENT_HANDLER step. Commit/abort machinery
     * re-reads the freshest version on purpose (the rebase mechanism) and would
     * false-fire; a compensation read is undo bookkeeping, not a business
     * observation;</li>
     * <li>the writer must be a FUNCTIONALITY step — only saga functionalities
     * enter the compensation path, so only their writes can be doomed. A read
     * that resolved to the writer's own COMPENSATION write observed the RESTORED
     * (recovered) state, which is not dirty, even though that functionality is in
     * the compensated set; and an EVENT_HANDLER write commits within its own step
     * and is never undone;</li>
     * <li>the writer must belong to a foreign functionality — a functionality
     * reading its own to-be-compensated write is not an isolation anomaly;</li>
     * <li>the writer's functionality must have entered the compensation path.
     * Entering it is enough: even when the compensation itself then fails, the
     * read was still of data the system tried to undo.</li>
     * </ul>
     */
    private static List<Anomaly> detectDirtyReads(
            List<ResolvedRead> resolvedReads, Set<FunctionalityId> compensatedFunctionalities) {

        // set for dedup: multiple reads from the same step to
        // the same doomed write yield one anomaly only
        Set<Anomaly> dirtyReads = new LinkedHashSet<>(); // preserves detection order

        for (ResolvedRead resolvedRead : resolvedReads) {
            StepEffect read = resolvedRead.read();
            StepEffect writer = resolvedRead.writer();

            if (!isApplicationEffect(read.stepKind())) {
                continue; // machinery reads see the freshest version; compensation reads are undo bookkeeping
            }
            if (writer == null || writer.stepKind() != StepKind.FUNCTIONALITY) {
                continue; // only functionality writes can be doomed; initial/compensation state is not
            }
            if (writer.functionalityId().equals(read.functionalityId())) {
                continue; // reading one's own doomed write is not an isolation anomaly
            }
            if (!compensatedFunctionalities.contains(writer.functionalityId())) {
                continue; // the write was never (nor attempted to be) undone
            }

            dirtyReads.add(new Anomaly.DirtyRead(read.stepId(), writer.stepId(), read.aggregateType()));
        }

        return List.copyOf(dirtyReads);
    }

    /**
     * NON_REPEATABLE_READ: two reads of the same aggregate, within the same
     * application functionality, observed versions produced by different
     * writers — the later one by a foreign functionality. Whatever the earlier
     * read validated or extracted may no longer hold at the later read.
     * <p>
     * Constraints, each enforced below:
     * <ul>
     * <li>both reads must be {@linkplain #isApplicationEffect application}
     * effects — FUNCTIONALITY or EVENT_HANDLER steps. The commit machinery
     * re-reads the freshest version by design (rebase), so including its reads
     * would flag every healed rebase as a non-repeatable read; a compensation
     * read is undo bookkeeping;</li>
     * <li>the reads must resolve to different writers — two reads of the same
     * unchanged version are repeatable;</li>
     * <li>the later read's writer must be a foreign functionality — when the
     * functionality itself wrote the aggregate between its own reads, seeing
     * its own write is the expected behavior.</li>
     * </ul>
     * Unlike the reads, the interposing (second) writer is deliberately NOT
     * restricted by kind. Any foreign version the reader observed between its two
     * reads is a genuinely different version, whether it was produced by a
     * functionality, an event handler, or the undo of a foreign
     * compensation/abort. This is why, for example, a departure that a foreign
     * saga later rolls back still surfaces here: the reader saw the initial
     * version, then the abort-restored version — two different versions.
     * <p>
     * Consecutive reads of the same (functionality, aggregate) are compared
     * pairwise in sequence order, so each foreign interposition is reported once:
     * the map holds the previous read of the pair, and every read replaces it, so
     * a read is only ever compared against the immediately preceding one.
     */
    private static List<Anomaly> detectNonRepeatableReads(List<ResolvedRead> resolvedReads) {
        Set<Anomaly> nonRepeatableReads = new LinkedHashSet<>(); // preserves detection order

        // (functionality, aggregateId) -> the previous resolved read of that pair
        Map<FunctionalityAggregateKey, ResolvedRead> previousReads = new HashMap<>();

        for (ResolvedRead current : resolvedReads) {
            StepEffect read = current.read();

            if (!isApplicationEffect(read.stepKind())) {
                continue; // machinery reads see the freshest version; compensation reads are undo bookkeeping
            }

            var key = new FunctionalityAggregateKey(read.functionalityId(), read.aggregateId());
            ResolvedRead previous = previousReads.put(key, current);
            if (previous == null) {
                continue; // first read of this aggregate by this functionality
            }

            StepEffect previousWriter = previous.writer();
            StepEffect currentWriter = current.writer();

            if (sameWriterStep(previousWriter, currentWriter)) {
                continue; // both reads saw the same version: repeatable
            }
            if (currentWriter == null || currentWriter.functionalityId().equals(read.functionalityId())) {
                continue; // the functionality re-reading its own write is expected behavior
            }

            nonRepeatableReads.add(new Anomaly.NonRepeatableRead(
                    read.functionalityId(),
                    previous.read().stepId(),
                    read.stepId(),
                    previousWriter == null ? StepId.forInitialStateSetupStep() : previousWriter.stepId(),
                    currentWriter.stepId(),
                    read.aggregateType()));
        }

        return List.copyOf(nonRepeatableReads);
    }

    private record FunctionalityAggregateKey(FunctionalityId functionalityId, Integer aggregateId) {
    }

    private static boolean sameWriterStep(@Nullable StepEffect writerA, @Nullable StepEffect writerB) {
        if (writerA == null || writerB == null) {
            return writerA == writerB; // both initial state, or only one of them
        }
        return writerA.stepId().equals(writerB.stepId());
    }

    /**
     * WRITE_SKEW: two committed functionalities each read state the other one
     * subsequently overwrote (mutual rw anti-dependencies) on distinct
     * aggregates, without ever seeing each other's writes. Each decided on a
     * snapshot the other invalidated, and both decisions committed — the
     * classic pattern where each write is locally valid but the pair breaks a
     * multi-aggregate constraint no serial order could break.
     * <p>
     * Constraints enforced:
     * <ul>
     * <li>rw edges are built from {@linkplain #isApplicationEffect application}
     * reads and application writes (FUNCTIONALITY or EVENT_HANDLER) — the
     * commit/abort/compensation effects are the oracle's rebase/undo
     * bookkeeping, not application decisions, and create no rw edges;</li>
     * <li>the two rw edges must be on distinct aggregates — the intra-aggregate
     * variant is exactly what the
     * 3-way rebase already serializes, so
     * requiring distinct aggregates targets the reachable case;</li>
     * <li>no wr edge may exist between the pair in either direction — a
     * functionality that read the other's write was (partially) serialized
     * after it, not mutually blind. The wr edge is detected against a writer of
     * ANY kind on purpose: seeing even a machinery write of the other party (its
     * commit or abort) is still evidence the reader was not blind to it, so the
     * broad detection keeps the rule conservative (it excludes more candidates,
     * never fabricates a skew);</li>
     * <li>both functionalities must have committed — if either compensated,
     * the skewed pair of decisions never materialized (doomed-data reads are
     * DIRTY_READ's territory).</li>
     * </ul>
     *
     * <b>Note:</b>
     * An rw-edge (read-write anti-dependency) occurs when functionality A reads an
     * aggregate's state, and functionality B subsequently modifies that exact same
     * aggregate. Logically, this means A operated on an older snapshot and must be
     * serialized <i>before</i> B. If B also has an rw-edge back to A on a different
     * aggregate, they form an impossible cycle where both must precede the other —
     * the exact structural definition of a write-skew anomaly.
     */
    private static List<Anomaly> detectWriteSkews(
            List<StepEffect> effectSequence,
            List<ResolvedRead> resolvedReads,
            Set<FunctionalityId> committedFunctionalities) {

        Map<FunctionalityPair, Map<Integer, String>> rwWitnessesByEdge =
                buildRwWitnessesByEdge(effectSequence, resolvedReads);
        Set<FunctionalityPair> wrConnectedPairs = buildWrConnectedPairs(resolvedReads);

        Set<Anomaly> writeSkews = new LinkedHashSet<>();
        // pairs reported once, regardless of (A,B)/(B,A) iteration order
        Set<FunctionalityPair> reportedPairs = new LinkedHashSet<>();

        for (Entry<FunctionalityPair, Map<Integer, String>> edge : rwWitnessesByEdge.entrySet()) {
            FunctionalityPair aToB = edge.getKey();
            FunctionalityPair bToA = aToB.reversed();

            if (reportedPairs.contains(aToB)) {
                continue; // pair already reported in the other orientation
            }
            if (wrConnectedPairs.contains(aToB)) {
                continue; // one saw the other's write: partially serialized, not mutually blind
            }
            if (!committedFunctionalities.contains(aToB.from())
                    || !committedFunctionalities.contains(aToB.to())) {
                continue; // a compensated functionality never materialized its decision
            }

            Map<Integer, String> witnessesOverwrittenByA = rwWitnessesByEdge.get(bToA);
            if (witnessesOverwrittenByA == null) {
                continue; // no mutual rw: plain anti-dependency, serializable
            }

            // Distinct aggregates required: the intra-aggregate (single shared
            // aggregate) variant is what the rebase machinery already serializes.
            // Note the requirement is on aggregate INSTANCES, not types — two
            // distinct aggregates of the same type are a valid skew.
            WitnessPair witnesses = pickDistinctWitnesses(edge.getValue(), witnessesOverwrittenByA);
            if (witnesses == null) {
                continue; // both edges witnessed only the same single aggregate
            }

            reportedPairs.add(aToB);
            reportedPairs.add(bToA);

            writeSkews.add(new Anomaly.WriteSkew(
                    aToB.from(), aToB.to(),
                    witnesses.aggregateTypeOverwrittenByB(), witnesses.aggregateTypeOverwrittenByA()));
        }

        return List.copyOf(writeSkews);
    }

    /**
     * The run's rw edges, as reader functionality -> overwriter functionality,
     * keeping every witness aggregate (id -> type) that justified each edge. An
     * application read of aggregate X creates an rw edge towards every foreign
     * functionality whose application step writes X later in the sequence: the
     * read observed a version that write invalidated.
     * <p>
     * Both the returned map and each witness map are insertion-ordered: they are
     * iterated when picking the witness pair to report, and a {@code HashMap}
     * would make the reported witnesses (and the order of the resulting anomalies)
     * vary run to run for one and the same effect sequence.
     */
    private static Map<FunctionalityPair, Map<Integer, String>> buildRwWitnessesByEdge(
            List<StepEffect> effectSequence, List<ResolvedRead> resolvedReads) {

        Map<FunctionalityPair, Map<Integer, String>> rwWitnessesByEdge = new LinkedHashMap<>();

        for (ResolvedRead resolvedRead : resolvedReads) {
            StepEffect read = resolvedRead.read();

            if (!isApplicationEffect(read.stepKind())) {
                continue; // machinery/undo reads are bookkeeping, not application decisions
            }

            for (StepEffect laterEffect : effectSequence) {
                if (!invalidates(laterEffect, read)) {
                    continue;
                }

                rwWitnessesByEdge
                        .computeIfAbsent(
                                new FunctionalityPair(read.functionalityId(), laterEffect.functionalityId()),
                                pair -> new LinkedHashMap<>())
                        .putIfAbsent(read.aggregateId(), read.aggregateType());
            }
        }

        return rwWitnessesByEdge;
    }

    /**
     * Whether {@code laterEffect} is a foreign application write that invalidated
     * the version {@code read} observed — i.e. whether the pair forms an rw edge.
     */
    private static boolean invalidates(StepEffect laterEffect, StepEffect read) {
        return laterEffect.sequenceNumber() > read.sequenceNumber()
                && laterEffect.isWrite()
                && isApplicationEffect(laterEffect.stepKind())
                && laterEffect.aggregateId().equals(read.aggregateId())
                && !laterEffect.functionalityId().equals(read.functionalityId());
    }

    /**
     * The functionality pairs connected by a wr edge, stored in both orders so a
     * single {@code contains} answers "did either of these two see the other's
     * write?". The writer's kind is deliberately not restricted — see the
     * constraint list on {@link #detectWriteSkews}.
     */
    private static Set<FunctionalityPair> buildWrConnectedPairs(List<ResolvedRead> resolvedReads) {
        Set<FunctionalityPair> wrConnectedPairs = new LinkedHashSet<>();

        for (ResolvedRead resolvedRead : resolvedReads) {
            StepEffect read = resolvedRead.read();
            StepEffect writer = resolvedRead.writer();

            if (!isApplicationEffect(read.stepKind())) {
                continue; // machinery/undo reads are bookkeeping, not application decisions
            }
            if (writer == null || writer.functionalityId().equals(read.functionalityId())) {
                continue; // initial state, or the functionality reading its own write
            }

            wrConnectedPairs.add(new FunctionalityPair(read.functionalityId(), writer.functionalityId()));
            wrConnectedPairs.add(new FunctionalityPair(writer.functionalityId(), read.functionalityId()));
        }

        return wrConnectedPairs;
    }

    private record WitnessPair(String aggregateTypeOverwrittenByB, String aggregateTypeOverwrittenByA) {
    }

    /**
     * Picks one witness aggregate per rw direction such that the two witnesses
     * are distinct aggregate instances; {@code null} when impossible (both
     * edges only witnessed the exact same aggregate).
     */
    private static @Nullable WitnessPair pickDistinctWitnesses(
            Map<Integer, String> witnessesOverwrittenByB, Map<Integer, String> witnessesOverwrittenByA) {

        for (Entry<Integer, String> witnessB : witnessesOverwrittenByB.entrySet()) {
            for (Entry<Integer, String> witnessA : witnessesOverwrittenByA.entrySet()) {
                if (!witnessB.getKey().equals(witnessA.getKey())) {
                    return new WitnessPair(witnessB.getValue(), witnessA.getValue());
                }
            }
        }
        return null;
    }

    /** A directed functionality pair (an edge of the dependency graph). */
    private record FunctionalityPair(FunctionalityId from, FunctionalityId to) {

        FunctionalityPair reversed() {
            return new FunctionalityPair(to, from);
        }
    }
}
