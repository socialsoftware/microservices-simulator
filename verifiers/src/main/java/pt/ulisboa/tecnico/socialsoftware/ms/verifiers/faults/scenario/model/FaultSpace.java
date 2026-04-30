package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.ArrayList;
import java.util.List;

public record FaultSpace(
        int length,
        List<String> scheduledStepIds,
        String defaultVector) {

    public FaultSpace {
        length = Math.max(0, length);
        scheduledStepIds = scheduledStepIds == null ? List.of() : List.copyOf(scheduledStepIds);
        defaultVector = defaultVector == null || defaultVector.isBlank()
                ? zeroVector(length)
                : defaultVector;
    }

    public static FaultSpace fromScheduledSteps(List<ScheduledStep> scheduledSteps) {
        List<String> stepIds = collectStepIds(scheduledSteps);
        return new FaultSpace(stepIds.size(), stepIds, null);
    }

    private static List<String> collectStepIds(List<ScheduledStep> scheduledSteps) {
        if (scheduledSteps == null || scheduledSteps.isEmpty()) {
            return List.of();
        }

        List<String> stepIds = new ArrayList<>(scheduledSteps.size());
        for (int index = 0; index < scheduledSteps.size(); index++) {
            ScheduledStep step = scheduledSteps.get(index);
            String id = step == null ? null : normalize(step.deterministicId());
            if (id == null) {
                id = "step-" + index;
            }
            stepIds.add(id);
        }

        return List.copyOf(stepIds);
    }

    private static String zeroVector(int length) {
        return "0".repeat(Math.max(0, length));
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
