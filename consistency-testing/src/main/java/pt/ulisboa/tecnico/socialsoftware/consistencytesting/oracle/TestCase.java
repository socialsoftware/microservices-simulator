package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public final class TestCase {

    private final Map<FunctionalityId, WorkflowFunctionality> functionalities;
    private final StepDependencies interDependencies;

    private TestCase(
            Map<FunctionalityId, WorkflowFunctionality> functionalities,
            StepDependencies interDependencies) {

        this.functionalities = Map.copyOf(functionalities);
        this.interDependencies = StepDependencies.copyOf(interDependencies);
    }

    public Map<FunctionalityId, WorkflowFunctionality> getFunctionalities() {
        return functionalities;
    }

    public StepDependencies getInterDependencies() {
        return interDependencies;
    }

    public static class Builder {

        private final Map<FunctionalityId, WorkflowFunctionality> functionalities = new HashMap<>();
        private final StepDependencies interDependencies = new StepDependencies();

        public Builder addFunctionality(
                FunctionalityId functionalityId, WorkflowFunctionality functionality) {

            if (functionalities.containsKey(functionalityId)) {
                throw new IllegalArgumentException(
                        "Functionality with ID '%s' already exists in the test case"
                                .formatted(functionalityId));
            }

            for (var entry : functionalities.entrySet()) {
                if (entry.getValue().equals(functionality)) {
                    throw new IllegalArgumentException(
                            "The provided functionality instance is already registered under a different ID: '%s'"
                                    .formatted(entry.getKey()));
                }
            }

            functionalities.put(functionalityId, functionality);
            return this;
        }

        // ! TODO add validation for event can't depend on emitting step,
        // ! nor on the steps before emitting step from the same functionality
        public Builder addInterDependency(StepId stepId, StepId dependsOnStepId) {
            if (stepId.equals(dependsOnStepId)) {
                throw new IllegalArgumentException(
                        "A step cannot depend on itself: '%s'".formatted(stepId));
            }

            if (stepId.getFunctionalityId().equals(dependsOnStepId.getFunctionalityId())) {
                throw new IllegalArgumentException(
                        "'%s' can't have an inter-dependency on '%s' because both belong to functionality '%s'"
                                .formatted(stepId, dependsOnStepId, stepId.getFunctionalityId()));
            }

            interDependencies.addStepDependencies(stepId, Set.of(dependsOnStepId));
            return this;
        }

        public TestCase build() {
            return new TestCase(functionalities, interDependencies);
        }
    }
}