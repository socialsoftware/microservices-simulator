package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

record TestCase(
        List<WorkflowFunctionality> functionalities,

        Map<FlowStep, Set<FlowStep>> interDependencies, // TODO should become AbstractSchedule
        Runnable setup,
        Runnable tearDown) {

    TestCase {
        functionalities = Objects.requireNonNull(List.copyOf(functionalities));
        interDependencies = Objects.requireNonNull(Map.copyOf(interDependencies));
    }
}
