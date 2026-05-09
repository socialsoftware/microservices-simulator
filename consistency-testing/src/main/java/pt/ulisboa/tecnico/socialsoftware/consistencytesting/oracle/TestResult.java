package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

record TestResult(
        // TODO this could capture the executed event handlers and compensations
        List<WorkflowFunctionality> executedFunctionalities,

        List<FlowStep> schedule,
        // TODO exceptions can become memory heavy, this could be optimized memory-wise
        Map<FlowStep, Exception> exceptions,
        Set<TestStatus> statuses) {

    TestResult {
        executedFunctionalities = Objects.requireNonNull(List.copyOf(executedFunctionalities));
        schedule = Objects.requireNonNull(List.copyOf(schedule));
        exceptions = Objects.requireNonNull(Map.copyOf(exceptions));
        statuses = Objects.requireNonNull(Set.copyOf(statuses));
    }
}
