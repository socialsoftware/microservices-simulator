package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A directed <em>happens-before</em> graph over {@link StepId}s, used to reason
 * about step ordering constraints <em>without</em> executing a schedule.
 * <p>
 * An edge {@code u -> v} means "{@code u} must execute before {@code v}". A
 * dependency "{@code dependent} depends on {@code dependsOn}" is therefore
 * stored as the edge {@code dependsOn -> dependent}.
 * <p>
 * This is the tool that lets callers keep a set of ordering constraints
 * satisfiable. A set of constraints whose happens-before graph contains a cycle
 * is a deadlock: every step on the cycle waits (transitively) on itself and can
 * never be released. The oracle only discovers such an unsatisfiable set after
 * a full, expensive run, where it surfaces as
 * {@link TestStatus#INTERDEPENDENCY_RESOLUTION_FAILED}. Querying
 * {@link #wouldCreateCycle} before committing to a constraint lets a caller
 * (the {@code TestDriver}, or {@link TestCase.Builder}) discard the bad
 * constraint cheaply instead.
 * <p>
 * The graph is intentionally a plain reachability structure rather than a
 * stateful scheduler: it knows nothing about success/failure semantics of
 * intra- vs inter-dependencies, only about "must run before". That keeps it a
 * sound over-approximation usable for both kinds of edge.
 */
public final class StepDependencyGraph {

    /**
     * node -> nodes that must run strictly after it (successors of "before" edges).
     */
    private final Map<StepId, Set<StepId>> successors = new HashMap<>();

    public StepDependencyGraph() {
    }

    /** Creates an independent deep copy of {@code other}. */
    public StepDependencyGraph(StepDependencyGraph other) {
        other.successors.forEach((node, succ) -> successors.put(node, new HashSet<>(succ)));
    }

    /**
     * Records that {@code dependent} depends on {@code dependsOn}, i.e. adds the
     * edge {@code dependsOn -> dependent} ("dependsOn before dependent").
     */
    public StepDependencyGraph addDependency(StepId dependent, StepId dependsOn) {
        successors.computeIfAbsent(dependsOn, key -> new HashSet<>()).add(dependent);
        successors.computeIfAbsent(dependent, key -> new HashSet<>());
        return this;
    }

    /**
     * Adds every "key depends on value" relation held by {@code dependencies} as
     * happens-before edges.
     */
    public StepDependencyGraph addAll(StepDependencies dependencies) {
        for (StepId dependent : dependencies.getSteps()) {
            for (StepId dependsOn : dependencies.getStepDependencies(dependent)) {
                addDependency(dependent, dependsOn);
            }
        }
        return this;
    }

    /**
     * @return {@code true} if a directed path of one or more edges leads from
     *         {@code source} to {@code target} (i.e. {@code source} must run,
     *         transitively, before {@code target}).
     */
    public boolean hasPath(StepId source, StepId target) {
        Deque<StepId> toVisit = new ArrayDeque<>(successors.getOrDefault(source, Set.of()));
        Set<StepId> visited = new HashSet<>();

        while (!toVisit.isEmpty()) {
            StepId current = toVisit.poll();
            if (current.equals(target)) {
                return true;
            }
            if (visited.add(current)) {
                toVisit.addAll(successors.getOrDefault(current, Set.of()));
            }
        }
        return false;
    }

    /**
     * @return {@code true} if recording "{@code dependent} depends on
     *         {@code dependsOn}" would close a cycle. That is the case when the
     *         dependency is reflexive, or when {@code dependent} already
     *         (transitively) has to run before {@code dependsOn} — adding the
     *         reverse edge {@code dependsOn -> dependent} would then make them
     *         mutually preceding.
     */
    public boolean wouldCreateCycle(StepId dependent, StepId dependsOn) {
        return dependent.equals(dependsOn) || hasPath(dependent, dependsOn);
    }

    /**
     * @return {@code true} if the constraint "{@code dependent} depends on
     *         {@code dependsOn}" is already implied by the current edges, i.e.
     *         {@code dependsOn} already (transitively) runs before
     *         {@code dependent}. Adding such an edge is harmless but pointless.
     */
    public boolean isRedundantDependency(StepId dependent, StepId dependsOn) {
        return hasPath(dependsOn, dependent);
    }
}
