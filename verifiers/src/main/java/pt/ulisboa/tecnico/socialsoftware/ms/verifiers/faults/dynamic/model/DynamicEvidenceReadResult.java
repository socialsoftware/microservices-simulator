package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.nio.file.Path;
import java.util.List;

public record DynamicEvidenceReadResult(
        List<DynamicEvidenceEvent> events,
        List<String> warnings,
        int evidenceFilesRead,
        int dynamicEventsRead,
        int eventsMissingTestContext,
        long evidenceBytesRead,
        List<Path> rawEventFiles) {
    public DynamicEvidenceReadResult {
        events = events == null ? List.of() : List.copyOf(events);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
        rawEventFiles = rawEventFiles == null ? List.of() : List.copyOf(rawEventFiles);
    }
}
