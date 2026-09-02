package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record DynamicEvidenceJoinResult(
        List<DynamicObservation> observations,
        List<DynamicAttributionLink> attributions,
        Map<String, Object> dynamicAccounting,
        List<String> diagnostics,
        int evidenceFilesRead,
        long evidenceBytesRead) {
    public DynamicEvidenceJoinResult {
        observations = observations == null ? List.of() : List.copyOf(observations);
        attributions = attributions == null ? List.of() : List.copyOf(attributions);
        dynamicAccounting = dynamicAccounting == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(dynamicAccounting));
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
