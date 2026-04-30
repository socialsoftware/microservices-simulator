package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record SourceModeClassification(
        SourceMode sourceMode,
        SourceModeConfidence confidence,
        List<String> evidence,
        SourceModeRejectionReason rejectionReason) {

    public SourceModeClassification {
        sourceMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        confidence = confidence == null ? SourceModeConfidence.UNKNOWN : confidence;
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }

    public static SourceModeClassification unknown() {
        return new SourceModeClassification(
                SourceMode.UNKNOWN,
                SourceModeConfidence.UNKNOWN,
                List.of(),
                null
        );
    }

    public boolean isUnknown() {
        return sourceMode == SourceMode.UNKNOWN;
    }

    public String warningText() {
        if (isUnknown()) {
            return "Source mode could not be proven; accepted by default unknown-mode policy.";
        }

        if (rejectionReason != null) {
            return "Source mode " + sourceMode + " rejected for saga catalog: " + rejectionReason;
        }

        return "Source mode " + sourceMode + " classified with confidence " + confidence;
    }
}
