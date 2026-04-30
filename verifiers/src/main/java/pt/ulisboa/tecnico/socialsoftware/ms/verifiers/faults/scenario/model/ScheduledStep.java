package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record ScheduledStep(
        String deterministicId,
        String sagaInstanceId,
        String stepId,
        int scheduleOrder,
        List<String> warnings) {

    public ScheduledStep {
        deterministicId = normalize(deterministicId);
        sagaInstanceId = normalize(sagaInstanceId);
        stepId = normalize(stepId);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
