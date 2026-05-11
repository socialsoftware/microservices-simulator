package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record DynamicEvidenceReadResult(List<DynamicEvidenceEvent> events, List<String> warnings, int evidenceFilesRead) {
    public DynamicEvidenceReadResult {
        events = events == null ? List.of() : List.copyOf(events);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
