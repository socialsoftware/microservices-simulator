package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Objects;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public record TestCase(
        List<WorkflowFunctionality> functionalities,

        StepDependencies interDependencies 
) {
    public TestCase {
        functionalities = Objects.requireNonNull(List.copyOf(functionalities));
        interDependencies = Objects.requireNonNull(interDependencies);
    }
}
