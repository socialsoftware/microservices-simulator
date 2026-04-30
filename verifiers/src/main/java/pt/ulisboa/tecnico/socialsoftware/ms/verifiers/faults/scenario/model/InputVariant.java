package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeConfidence;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record InputVariant(
        String deterministicId,
        String sagaFqn,
        String sourceClassFqn,
        String sourceMethodName,
        String sourceBindingName,
        InputResolutionStatus resolutionStatus,
        SourceMode sourceMode,
        SourceModeConfidence sourceModeConfidence,
        List<String> sourceModeEvidence,
        String stableSourceText,
        String provenanceText,
        List<String> constructorArgumentSummaries,
        Map<String, String> logicalKeyBindings,
        List<String> warnings) {

    public InputVariant {
        deterministicId = normalize(deterministicId);
        sagaFqn = normalize(sagaFqn);
        sourceClassFqn = normalize(sourceClassFqn);
        sourceMethodName = normalize(sourceMethodName);
        sourceBindingName = normalize(sourceBindingName);
        resolutionStatus = resolutionStatus == null ? InputResolutionStatus.UNRESOLVED : resolutionStatus;
        sourceMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        sourceModeConfidence = sourceModeConfidence == null ? SourceModeConfidence.UNKNOWN : sourceModeConfidence;
        sourceModeEvidence = sourceModeEvidence == null ? List.of() : List.copyOf(sourceModeEvidence);
        stableSourceText = normalize(stableSourceText);
        provenanceText = normalize(provenanceText);
        constructorArgumentSummaries = constructorArgumentSummaries == null ? List.of() : List.copyOf(constructorArgumentSummaries);
        logicalKeyBindings = logicalKeyBindings == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(logicalKeyBindings));
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public InputVariant(String deterministicId,
                        String sagaFqn,
                        String sourceClassFqn,
                        String sourceMethodName,
                        String sourceBindingName,
                        InputResolutionStatus resolutionStatus,
                        String stableSourceText,
                        String provenanceText,
                        List<String> constructorArgumentSummaries,
                        Map<String, String> logicalKeyBindings,
                        List<String> warnings) {
        this(deterministicId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                resolutionStatus,
                SourceMode.UNKNOWN,
                SourceModeConfidence.UNKNOWN,
                List.of(),
                stableSourceText,
                provenanceText,
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
