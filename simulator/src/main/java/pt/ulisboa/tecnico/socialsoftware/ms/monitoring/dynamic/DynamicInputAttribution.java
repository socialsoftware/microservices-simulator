package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.dynamic;

import java.util.List;

public record DynamicInputAttribution(
        String inputVariantId,
        String status,
        String basis,
        List<String> candidateInputVariantIds) {

    public static final String STATUS_MATCHED = "MATCHED";
    public static final String STATUS_NO_MATCH = "NO_MATCH";
    public static final String STATUS_AMBIGUOUS = "AMBIGUOUS";

    public DynamicInputAttribution {
        candidateInputVariantIds = candidateInputVariantIds == null ? List.of() : List.copyOf(candidateInputVariantIds);
    }

    public static DynamicInputAttribution disabled() {
        return new DynamicInputAttribution(null, null, null, List.of());
    }

    public static DynamicInputAttribution noMatch(String basis) {
        return new DynamicInputAttribution(null, STATUS_NO_MATCH, basis, List.of());
    }

    public static DynamicInputAttribution ambiguous(String basis, List<String> candidateInputVariantIds) {
        return new DynamicInputAttribution(null, STATUS_AMBIGUOUS, basis, candidateInputVariantIds);
    }

    public static DynamicInputAttribution matched(String basis, String inputVariantId) {
        return new DynamicInputAttribution(inputVariantId, STATUS_MATCHED, basis, List.of(inputVariantId));
    }
}
