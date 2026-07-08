package pt.ulisboa.tecnico.socialsoftware.ms.faults;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

public class InMemoryFaultVectorProvider implements FaultVectorFaultProvider {
    private final Map<Integer, FaultVectorFault> assignments;

    public InMemoryFaultVectorProvider(Map<Integer, FaultVectorFault> assignments) {
        this.assignments = new LinkedHashMap<>(assignments);
    }

    @Override
    public Optional<FaultVectorFault> faultFor(FaultVectorBoundaryContext context) {
        FaultVectorFault assignment = assignments.get(context.slotIndex());
        if (assignment == null) {
            return Optional.empty();
        }
        if (!same(assignment.scenarioExecutionId(), context.scenarioExecutionId())
                || !same(assignment.scenarioPlanId(), context.scenarioPlanId())
                || !same(assignment.sagaInstanceId(), context.sagaInstanceId())
                || !same(assignment.scheduledStepId(), context.scheduledStepId())
                || !same(assignment.runtimeStepName(), context.runtimeStepName())) {
            return Optional.empty();
        }
        return Optional.of(assignment);
    }

    private boolean same(String left, String right) {
        return left == null ? right == null : left.equals(right);
    }
}
