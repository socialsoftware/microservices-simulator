package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record EventEmissionSiteFact(
        String sourceServiceClassFqn,
        String sourceServiceMethodSignature,
        int emissionOrdinal,
        String eventTypeFqn,
        List<String> extractionEvidence) {

    public EventEmissionSiteFact {
        extractionEvidence = extractionEvidence == null ? List.of() : List.copyOf(extractionEvidence);
    }
}
