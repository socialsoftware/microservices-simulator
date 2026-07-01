package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;

public record TestResult(
        // TODO add Anomalies detected

        StepDependencies intraDependencies,
        StepDependencies interDependencies,
        Map<FunctionalityId, WorkflowFunctionality> functionalities,
        List<StepId> schedule,

        // TODO exceptions can become memory heavy, this could be optimized memory-wise
        Map<StepId, Exception> exceptions,
        Set<TestStatus> statuses,
        Set<ReadsFromRelation> readsFromRelations,

        // inter-invariant name -> the violations detected for it
        Map<String, Set<InterInvariantViolation>> interInvariantViolations) {

    public TestResult {
        intraDependencies = StepDependencies.copyOf(intraDependencies);
        interDependencies = StepDependencies.copyOf(interDependencies);
        functionalities = Objects.requireNonNull(Map.copyOf(functionalities));
        schedule = Objects.requireNonNull(List.copyOf(schedule));
        exceptions = Objects.requireNonNull(Map.copyOf(exceptions));
        statuses = Objects.requireNonNull(Set.copyOf(statuses));
        readsFromRelations = Objects.requireNonNull(Set.copyOf(readsFromRelations));
        interInvariantViolations = Objects.requireNonNull(Map.copyOf(interInvariantViolations));
    }

    /**
     * Inter-invariant name -> the violations detected for it after the schedule
     * finished.
     * <p>
     * Empty when no inter-invariant was broken.
     */
    public Map<String, Set<InterInvariantViolation>> interInvariantViolations() {
        return interInvariantViolations;
    }
}
