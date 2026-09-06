package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.FunctionalityId;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.StepEffect;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/**
 * The aggregate-level read/write footprint one functionality exhibited when it
 * ran alone (its set of {@link Access}es).
 */
public record FunctionalityFootprint(FunctionalityId functionalityId, Set<Access> accesses) {

    // TODO
    /*
     * KNOWN LIMITATION: normal catalog profiling observes only a functionality's
     * successful solo path. Writes that exist only on an abort or compensation path
     * are therefore invisible, so a pair conflicting only through them can be
     * wrongly pruned. The TestDriver's lenient, test-only profiling path can capture
     * those effects when a targeted functionality already aborts alone, but it does
     * not cover functionalities that abort only under contention.
     * <ul>
     * <li>TODO the group-level emergent-effects (feed the effects observed in
     * group runs back into the footprints) is the principled fix: it would pick up
     * contention-only writes the first time a group run exhibits them.</li>
     * <li>TODO cheaper alternative: also profile each functionality once with
     * a forced abort to capture its compensation footprint (even though they
     * migh not naturally be triggered during execution).</li>
     * </ul>
     */

    public FunctionalityFootprint {
        Objects.requireNonNull(functionalityId);
        accesses = Set.copyOf(accesses);
    }

    /**
     * One distinct aggregate-level access. {@code identity} is the registered
     * handle when the aggregate was created by the initial-state setup
     * ({@code handleBased} true); otherwise the aggregate was created mid-run
     * and the identity falls back to its {@code aggregateType}.
     * <p>
     * A type-fallback access is matched as a WILDCARD over its type (see
     * {@link FunctionalityGroupPlanner}): a mid-run-created aggregate CAN be
     * reached by another functionality (event handlers, queries over all
     * aggregates of an execution), and there is no id-level way to tell across
     * separate profiling runs, so the planner over-approximates rather than
     * silently pruning a real conflict. The cost is only possible wasted runs,
     * e.g. two functionalities that each create their own private aggregate of
     * the same type get paired.
     */
    public record Access(
            String identity,
            String aggregateType,
            boolean handleBased,
            StepEffect.EffectKind kind) {

        public boolean isWrite() {
            return kind == StepEffect.EffectKind.WRITE;
        }
    }

    /**
     * Builds the footprint of {@code functionalityId} from the
     * {@link TestResult} of its solo run, resolving each effect's run-local
     * {@code aggregateId} to a stable identity through {@code registry}.
     * <p>
     * Every effect of the run is attributed to the functionality: the run
     * contained nothing else, so event-handler steps (which carry synthetic
     * functionality ids) are its effects too.
     */
    public static FunctionalityFootprint fromSoloRun(
            FunctionalityId functionalityId, TestResult soloRunResult, AggregateHandlesRegistry registry) {

        Set<Access> accesses = new LinkedHashSet<>();
        for (StepEffect effect : soloRunResult.effectSequence()) {
            accesses.add(registry.handleOf(effect.aggregateId())
                    .map(handle -> new Access(handle, effect.aggregateType(), true, effect.effectKind()))
                    .orElseGet(() -> new Access(
                            effect.aggregateType(), effect.aggregateType(), false, effect.effectKind())));
        }

        return new FunctionalityFootprint(functionalityId, accesses);
    }

    public boolean writesAnything() {
        return accesses.stream().anyMatch(Access::isWrite);
    }
}
