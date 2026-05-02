package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.FlowStep;

record TestResult(
        List<FlowStep> schedule,
        // TODO exceptions can become memory heavy, this could be optimized memory-wise
        Map<FlowStep, Exception> exceptions,
        Set<TestStatus> statuses) {

    TestResult {
        schedule = Objects.requireNonNull(List.copyOf(schedule));
        exceptions = Objects.requireNonNull(Map.copyOf(exceptions));
        statuses = Objects.requireNonNull(Set.copyOf(statuses));
    }
}
