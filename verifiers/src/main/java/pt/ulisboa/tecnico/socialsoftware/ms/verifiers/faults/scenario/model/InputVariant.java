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
        String callContextMethodName,
        InputRole inputRole,
        FixtureOrigin fixtureOrigin,
        InputResolutionStatus resolutionStatus,
        SourceMode sourceMode,
        SourceModeConfidence sourceModeConfidence,
        List<String> sourceModeEvidence,
        String stableSourceText,
        String provenanceText,
        List<InputOwner> owners,
        List<String> constructorArgumentSummaries,
        Map<String, String> logicalKeyBindings,
        List<String> warnings,
        InputRecipe inputRecipe) {

    public InputVariant {
        deterministicId = normalize(deterministicId);
        sagaFqn = normalize(sagaFqn);
        sourceClassFqn = normalize(sourceClassFqn);
        sourceMethodName = normalize(sourceMethodName);
        sourceBindingName = normalize(sourceBindingName);
        callContextMethodName = normalize(callContextMethodName);
        inputRole = inputRole == null ? InputRole.UNKNOWN : inputRole;
        fixtureOrigin = fixtureOrigin == null ? FixtureOrigin.UNKNOWN : fixtureOrigin;
        resolutionStatus = resolutionStatus == null ? InputResolutionStatus.UNRESOLVED : resolutionStatus;
        sourceMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        sourceModeConfidence = sourceModeConfidence == null ? SourceModeConfidence.UNKNOWN : sourceModeConfidence;
        sourceModeEvidence = sourceModeEvidence == null ? List.of() : List.copyOf(sourceModeEvidence);
        stableSourceText = normalize(stableSourceText);
        provenanceText = normalize(provenanceText);
        owners = normalizeOwners(owners);
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
                        SourceMode sourceMode,
                        SourceModeConfidence sourceModeConfidence,
                        List<String> sourceModeEvidence,
                        String stableSourceText,
                        String provenanceText,
                        List<InputOwner> owners,
                        List<String> constructorArgumentSummaries,
                        Map<String, String> logicalKeyBindings,
                        List<String> warnings,
                        InputRecipe inputRecipe) {
        this(deterministicId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                null,
                InputRole.UNKNOWN,
                FixtureOrigin.UNKNOWN,
                resolutionStatus,
                sourceMode,
                sourceModeConfidence,
                sourceModeEvidence,
                stableSourceText,
                provenanceText,
                owners,
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings,
                inputRecipe);
    }

    public InputVariant(String deterministicId,
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
                        List<InputOwner> owners,
                        List<String> constructorArgumentSummaries,
                        Map<String, String> logicalKeyBindings,
                        List<String> warnings) {
        this(deterministicId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                resolutionStatus,
                sourceMode,
                sourceModeConfidence,
                sourceModeEvidence,
                stableSourceText,
                provenanceText,
                owners,
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings,
                null);
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
                        List<String> warnings,
                        InputRecipe inputRecipe) {
        this(deterministicId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                null,
                InputRole.UNKNOWN,
                FixtureOrigin.UNKNOWN,
                resolutionStatus,
                SourceMode.UNKNOWN,
                SourceModeConfidence.UNKNOWN,
                List.of(),
                stableSourceText,
                provenanceText,
                defaultOwners(sourceClassFqn, sourceMethodName),
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings,
                inputRecipe);
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
                stableSourceText,
                provenanceText,
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings,
                null);
    }

    public InputVariant(String deterministicId,
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
        this(deterministicId,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                null,
                InputRole.UNKNOWN,
                FixtureOrigin.UNKNOWN,
                resolutionStatus,
                sourceMode,
                sourceModeConfidence,
                sourceModeEvidence,
                stableSourceText,
                provenanceText,
                defaultOwners(sourceClassFqn, sourceMethodName),
                constructorArgumentSummaries,
                logicalKeyBindings,
                warnings,
                null);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static List<InputOwner> defaultOwners(String sourceClassFqn, String sourceMethodName) {
        String normalizedClass = normalize(sourceClassFqn);
        String normalizedMethod = normalize(sourceMethodName);
        if (normalizedClass == null || normalizedMethod == null) {
            return List.of();
        }
        return List.of(new InputOwner(normalizedClass, normalizedMethod));
    }

    private static List<InputOwner> normalizeOwners(List<InputOwner> owners) {
        if (owners == null || owners.isEmpty()) {
            return List.of();
        }
        return owners.stream()
                .filter(owner -> owner != null && owner.isComplete())
                .distinct()
                .toList();
    }
}
