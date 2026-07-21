package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record ScheduledStep(
        String deterministicId,
        String sagaInstanceId,
        String stepId,
        int scheduleOrder,
        String runtimeStepName,
        List<String> warnings) {

    public ScheduledStep(String deterministicId,
                         String sagaInstanceId,
                         String stepId,
                         int scheduleOrder,
                         List<String> warnings) {
        this(deterministicId, sagaInstanceId, stepId, scheduleOrder, runtimeStepNameFromStepId(stepId), warnings);
    }

    public ScheduledStep {
        deterministicId = normalize(deterministicId);
        sagaInstanceId = normalize(sagaInstanceId);
        stepId = normalize(stepId);
        runtimeStepName = normalize(runtimeStepName);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public static String runtimeStepNameFromStepId(String stepId) {
        String normalized = normalize(stepId);
        if (normalized == null) {
            return null;
        }
        int marker = normalized.lastIndexOf("::");
        if (marker < 0 || marker + 2 >= normalized.length()) {
            return null;
        }
        return normalize(normalized.substring(marker + 2).replaceFirst("#\\d+$", "").trim());
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
