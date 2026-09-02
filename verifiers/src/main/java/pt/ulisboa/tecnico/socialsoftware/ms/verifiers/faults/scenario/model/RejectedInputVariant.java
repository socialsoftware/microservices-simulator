package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeRejectionReason;

import java.util.List;

public record RejectedInputVariant(
        InputVariant inputVariant,
        SourceModeRejectionReason rejectionReason,
        List<String> warnings
) {
    public RejectedInputVariant {
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
