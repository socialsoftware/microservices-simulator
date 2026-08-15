package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record EventEmissionSite(
        String deterministicId,
        String sourceServiceClassFqn,
        String sourceServiceMethodSignature,
        int emissionOrdinal,
        String eventTypeFqn,
        List<String> extractionEvidence) {

    public EventEmissionSite {
        deterministicId = normalize(deterministicId);
        sourceServiceClassFqn = normalize(sourceServiceClassFqn);
        sourceServiceMethodSignature = normalize(sourceServiceMethodSignature);
        eventTypeFqn = normalize(eventTypeFqn);
        extractionEvidence = extractionEvidence == null ? List.of() : List.copyOf(extractionEvidence);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
