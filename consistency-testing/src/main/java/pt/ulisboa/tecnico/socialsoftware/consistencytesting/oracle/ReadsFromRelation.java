package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jspecify.annotations.Nullable;

/**
 * A reads-from relation observed during a run: {@code reader} read an aggregate
 * of {@code aggregateType} whose current value was produced by {@code writer}.
 * <p>
 * The writer is the step that last wrote that aggregate before the read; if the
 * aggregate was only ever written while building the initial state, the writer
 * is the {@link StepId#forInitialStateSetupStep()}.
 * <p>
 * The relation is keyed by {@code aggregateType} rather than aggregate id on
 * purpose: ids are reassigned every run, so type is what stays comparable
 * across runs and reports.
 * <p>
 * Relations are not tracked independently: they are a derived view over the
 * run's authoritative {@code effectSequence}, computed by
 * {@link #deriveAll(List)}.
 */
public record ReadsFromRelation(StepId reader, StepId writer, String aggregateType) {

    /**
     * Derives every reads-from relation of a run from its effect sequence:
     * <ul>
     * <li>the writer of a read is the step that most recently wrote that
     * {@code aggregateId};</li>
     * <li>a step reading an aggregate it has itself already written is not a
     * cross-step reads-from and produces no relation
     * (since each step executes at most once);</li>
     * <li>a read with no previous writer read the initial state, attributed to
     * {@link StepId#forInitialStateSetupStep()}.</li>
     * </ul>
     */
    public static Set<ReadsFromRelation> deriveAll(List<StepEffect> effectSequence) {
        Set<ReadsFromRelation> relations = new LinkedHashSet<>();

        for (ResolvedRead resolvedRead : resolveReads(effectSequence)) {
            StepEffect writer = resolvedRead.writer();
            StepId writerStepId = writer == null
                    ? StepId.forInitialStateSetupStep()
                    : writer.stepId();

            relations.add(new ReadsFromRelation(
                    resolvedRead.read().stepId(), writerStepId, resolvedRead.read().aggregateType()));
        }

        return relations;
    }

    /**
     * A read effect matched to the write effect it observed; {@code writer} is
     * {@code null} when the read observed the initial state (no earlier write
     * to that aggregate exists in the sequence).
     */
    record ResolvedRead(StepEffect read, @Nullable StepEffect writer) {
        // TODO could have a helper method that returns
        // * initialStateSetupStep() instead of null
        // StepId writerStepId() {
        // return writer == null ? StepId.forInitialStateSetupStep() : writer.stepId();
        // }
    }

    /**
     * Resolves, in sequence order, every cross-step read of the effect sequence
     * to the write it observed.
     * <p>
     * Reads of an aggregate the same step already wrote are self-reads and are
     * omitted (they carry no cross-step information, since each step executes at
     * most once).
     */
    static List<ResolvedRead> resolveReads(List<StepEffect> effectSequence) {
        List<ResolvedRead> resolvedReads = new ArrayList<>();

        // aggregateId -> the effect that most recently wrote it
        Map<Integer, StepEffect> lastWriterByAggregate = new HashMap<>();

        // (stepId, aggregateId) pairs already written, to skip self-reads.
        // Keyed by step (not "current step") because each step executes at most
        // once, so the pair uniquely identifies
        // "this step already wrote this aggregate".
        Set<SelfWriteKey> selfWrites = new HashSet<>();

        for (StepEffect effect : effectSequence) {
            if (effect.isWrite()) {
                lastWriterByAggregate.put(effect.aggregateId(), effect);
                selfWrites.add(new SelfWriteKey(effect.stepId(), effect.aggregateId()));
                continue;
            }

            if (selfWrites.contains(new SelfWriteKey(effect.stepId(), effect.aggregateId()))) {
                continue; // reading what this step itself wrote is not a cross-step reads-from
            }

            resolvedReads.add(new ResolvedRead(effect, lastWriterByAggregate.get(effect.aggregateId())));
        }

        return resolvedReads;
    }

    private record SelfWriteKey(StepId stepId, Integer aggregateId) {
    }
}
