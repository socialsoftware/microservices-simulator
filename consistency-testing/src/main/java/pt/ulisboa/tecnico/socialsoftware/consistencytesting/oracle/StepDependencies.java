package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;

public class StepDependencies {

    private final Map<FlowStep, Set<FlowStep>> stepDependencies = new HashMap<>();

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

    public void setStepDependencies(FlowStep step, Set<FlowStep> dependencies) {
        if (dependencies.contains(step)) {
            throw new IllegalArgumentException("Step '" + step.getName() + "' cannot depend on itself.");
        }
        stepDependencies.put(step, new HashSet<>(dependencies));
    }

    /**
     * It's the equivalent of a union of the dependency sets, and the final result
     * is stored in {@code this} object
     * 
     * @param other the other dependencies to be merged into this object
     */
    public void merge(StepDependencies other) {
        other.stepDependencies.forEach((key, value) -> {
            this.stepDependencies.merge(key, new HashSet<>(value), (thisDependencies, otherDependencies) -> {
                thisDependencies.addAll(otherDependencies);
                return thisDependencies;
            });
        });
    }
}
