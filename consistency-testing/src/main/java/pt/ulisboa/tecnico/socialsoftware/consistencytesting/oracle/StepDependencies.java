package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;

public class StepDependencies {

    private final Map<FlowStep, Set<FlowStep>> stepDependencies;

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
        other.stepDependencies.forEach((key, value) -> this.stepDependencies.put(key, new HashSet<>(value)));
    }

    private StepDependencies(Map<FlowStep, Set<FlowStep>> stepDependencies) {
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
        Map<FlowStep, Set<FlowStep>> stepDependenciesMap = new HashMap<>();
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
    public static StepDependencies of(Collection<FlowStep> steps) {
        StepDependencies stepDependencies = new StepDependencies();
        for (FlowStep step : steps) {
            stepDependencies.setStepDependencies(step, new HashSet<>(step.getDependencies()));
        }
        return stepDependencies;
    }

    public Set<FlowStep> getSteps() {
        return stepDependencies.keySet();
    }

    /**
     * @param step the step whose associated dependencies are to be returned
     * @return the step's dependency set, or an {@code empty} set if the step is not
     *         found
     */
    public Set<FlowStep> getStepDependencies(FlowStep step) {
        Set<FlowStep> dependencies = stepDependencies.get(step);
        if (dependencies == null) {
            dependencies = new HashSet<>();
        }
        return dependencies;
    }

    public StepDependencies setStepDependencies(FlowStep step, Set<FlowStep> dependencies) {
        assertStepWontDependOnItself(step, dependencies);
        stepDependencies.put(step, new HashSet<>(dependencies));
        return this;
    }

    public StepDependencies addStepDependencies(FlowStep step, Set<FlowStep> moreDependencies) {
        assertStepWontDependOnItself(step, moreDependencies);
        Set<FlowStep> currentDependencies = getStepDependencies(step);
        currentDependencies.addAll(moreDependencies);
        return this;
    }

    /**
     * It's the equivalent of a union of the dependency sets, and the final result
     * is stored in {@code this} object
     * 
     * @param other the other dependencies to be merged into this object
     */
    public StepDependencies merge(StepDependencies other) {
        other.stepDependencies.forEach((key, value) -> {
            this.stepDependencies.merge(key, new HashSet<>(value), (thisDependencies, otherDependencies) -> {
                thisDependencies.addAll(otherDependencies);
                return thisDependencies;
            });
        });
        return this;
    }

    private void assertStepWontDependOnItself(FlowStep step, Set<FlowStep> newDependencies) {
        if (newDependencies.contains(step)) {
            throw new IllegalArgumentException("Step %s cannot depend on itself.".formatted(step.getName()));
        }
    }
}
