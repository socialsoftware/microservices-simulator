package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record ConflictEvidence(
        String deterministicId,
        String leftScheduledStepId,
        String rightScheduledStepId,
        AggregateKey leftAggregateKey,
        AggregateKey rightAggregateKey,
        AccessMode leftAccessMode,
        AccessMode rightAccessMode,
        ConflictKind kind,
        List<String> warnings) {

    public ConflictEvidence {
        deterministicId = normalize(deterministicId);
        leftScheduledStepId = normalize(leftScheduledStepId);
        rightScheduledStepId = normalize(rightScheduledStepId);
        leftAccessMode = leftAccessMode == null ? AccessMode.WRITE : leftAccessMode;
        rightAccessMode = rightAccessMode == null ? AccessMode.WRITE : rightAccessMode;
        kind = kind == null ? ConflictKind.UNKNOWN : kind;
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
