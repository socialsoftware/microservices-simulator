package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class StepDependencies {

    private final Map<StepId, Set<StepId>> stepDependencies;

    public StepDependencies() {
        this.stepDependencies = new HashMap<>();
    }

    /**
     * Creates a mutable deep copy of another {@code StepDependencies} instance.
     * <p>
     * The returned object uses a mutable map and mutable dependency sets, so it can
     * be changed independently from {@code other}.
     *
     * @param other the instance to copy
     */
    public StepDependencies(StepDependencies other) {
        this.stepDependencies = new HashMap<>();
        other.stepDependencies.forEach(this::setStepDependencies);
    }

    private StepDependencies(Map<StepId, Set<StepId>> stepDependencies) {
        this.stepDependencies = stepDependencies;
    }

    /**
     * Creates an immutable deep copy of a {@code StepDependencies} instance.
     * <p>
     * The returned object is backed by an unmodifiable map and unmodifiable
     * dependency sets.
     *
     * @param original the instance to copy
     * @return an immutable copy of {@code original}
     */
    public static StepDependencies copyOf(StepDependencies original) {
        Map<StepId, Set<StepId>> stepDependenciesMap = new HashMap<>();
        original.stepDependencies.forEach((key, value) -> stepDependenciesMap.put(key, Set.copyOf(value)));
        return new StepDependencies(Map.copyOf(stepDependenciesMap));
    }

    /**
     * Creates a mutable {@code StepDependencies} instance from a collection of
     * steps.
     * <p>
     * Each step's dependency set is copied into a mutable set, and the resulting
     * structure is mutable.
     *
     * @param steps the steps used to initialize dependencies
     * @return a mutable instance initialized with each step dependencies
     */
    public static StepDependencies of(Collection<OracleStep> steps) {
        StepDependencies stepDependencies = new StepDependencies();
        for (OracleStep step : steps) {
            stepDependencies.setStepDependencies(step.getId(), step.getDependencies());
        }
        return stepDependencies;
    }

    /**
     * Returns a set containing all the step IDs that have registered dependencies.
     */
    public Set<StepId> getSteps() {
        return stepDependencies.keySet();
    }

    /**
     * @return the step's dependency set, or an {@code empty} set if not found
     */
    public Set<StepId> getStepDependencies(StepId stepId) {
        Set<StepId> dependencies = stepDependencies.get(stepId);
        return dependencies != null ? dependencies : new HashSet<>();
    }

    /**
     * Overwrites the dependency set for a specific step.
     * <p>
     * If the step already has associated dependencies, they are discarded and
     * replaced entirely with a mutable copy of the provided set.
     *
     * @param stepId       the ID of the step to configure
     * @param dependencies the new set of dependencies to assign
     * @return this {@code StepDependencies} instance for method chaining
     * @throws IllegalArgumentException if the step attempts to depend on itself
     */
    public StepDependencies setStepDependencies(StepId stepId, Set<StepId> dependencies) {
        assertStepWontDependOnItself(stepId, dependencies);
        stepDependencies.put(stepId, new HashSet<>(dependencies));
        return this;
    }

    /**
     * Adds dependencies to an existing step's dependency set.
     * <p>
     * If the step already has dependencies, the new dependencies are merged into
     * the existing set. If the step does not have any dependencies yet, a new
     * mutable set is initialized with the provided dependencies.
     *
     * @param stepId           the ID of the step to update
     * @param moreDependencies the dependencies to add
     * @return this {@code StepDependencies} instance for method chaining
     * @throws IllegalArgumentException if the step attempts to depend on itself
     */
    public StepDependencies addStepDependencies(StepId stepId, Set<StepId> moreDependencies) {
        assertStepWontDependOnItself(stepId, moreDependencies);
        Set<StepId> currentDependencies = stepDependencies.computeIfAbsent(stepId, key -> new HashSet<>());
        currentDependencies.addAll(moreDependencies);
        return this;
    }

    /**
     * It's the equivalent of a union of the dependency sets, and the final result
     * is stored in {@code this} object
     * 
     * @param other the other dependencies to be merged into {@code this} object
     */
    public StepDependencies merge(StepDependencies other) {
        other.stepDependencies.forEach(this::addStepDependencies);
        return this;
    }

    private void assertStepWontDependOnItself(StepId stepId, Set<StepId> newDependencies) {
        if (newDependencies.contains(stepId)) {
            throw new IllegalArgumentException("Step '%s' cannot depend on itself".formatted(stepId));
        }
    }
}
