package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import java.util.List;

public record DynamicEvidenceJoinResult(
        List<EnrichedScenarioRecord> records,
        List<String> warnings,
        int dynamicEventsRead,
        int eventsMissingTestContext,
        int evidenceFilesRead,
        long evidenceBytesRead) {
    public DynamicEvidenceJoinResult {
        records = records == null ? List.of() : List.copyOf(records);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}
