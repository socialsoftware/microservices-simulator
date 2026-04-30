package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RejectedInputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceModeRejectionReason;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class InputVariantNormalizer {

    private InputVariantNormalizer() {
    }

    public static NormalizationResult normalize(List<InputVariant> inputs, ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        List<InputVariant> safeInputs = inputs == null ? List.of() : inputs;

        LinkedHashMap<String, InputVariant> deduped = new LinkedHashMap<>();
        List<RejectedInputVariant> rejected = new ArrayList<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();

        int seen = 0;
        int excludedByPolicy = 0;
        int rejectedBySourceMode = 0;
        int deduplicated = 0;

        for (InputVariant rawInput : safeInputs) {
            if (rawInput == null) {
                warnings.add("ignored null input variant");
                continue;
            }

            seen++;
            InputVariant normalized = normalizeVariant(rawInput);
            if (normalized.sagaFqn() == null) {
                warnings.add("ignored input variant without saga FQN");
                continue;
            }

            SourceModeRejectionReason sourceModeRejectionReason = sourceModeRejectionReason(normalized.sourceMode());
            if (sourceModeRejectionReason != null) {
                rejectedBySourceMode++;
                rejected.add(new RejectedInputVariant(normalized, sourceModeRejectionReason,
                        List.of("rejected input variant " + normalized.deterministicId()
                                + " for saga " + normalized.sagaFqn()
                                + " due to source mode " + normalized.sourceMode())));
                warnings.add("rejected input variant " + normalized.deterministicId()
                        + " for saga " + normalized.sagaFqn()
                        + " due to source mode " + normalized.sourceMode());
                continue;
            }

            if (!isAllowedByPolicy(normalized.resolutionStatus(), effectiveConfig.inputPolicy())) {
                excludedByPolicy++;
                warnings.add("excluded input variant " + normalized.deterministicId()
                        + " for saga " + normalized.sagaFqn()
                        + " due to input policy " + effectiveConfig.inputPolicy().name());
                continue;
            }

            InputVariant existing = deduped.get(normalized.deterministicId());
            if (existing == null) {
                deduped.put(normalized.deterministicId(), normalized);
            } else {
                deduplicated++;
                warnings.add("deduplicated input variant " + normalized.deterministicId());
                deduped.put(normalized.deterministicId(), mergeAcceptedInputs(existing, normalized));
            }
        }

        List<InputVariant> accepted = deduped.values().stream()
                .map(InputVariantNormalizer::addUnknownSourceModeWarning)
                .collect(java.util.stream.Collectors.toCollection(ArrayList::new));
        accepted.sort(Comparator
                .comparing(InputVariant::sagaFqn, Comparator.nullsFirst(String::compareTo))
                .thenComparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo)));

        LinkedHashMap<String, List<InputVariant>> grouped = new LinkedHashMap<>();
        int capped = 0;
        int maxPerSaga = Math.max(0, effectiveConfig.maxInputVariantsPerSaga());
        for (InputVariant input : accepted) {
            grouped.computeIfAbsent(input.sagaFqn(), ignored -> new ArrayList<>()).add(input);
        }

        LinkedHashMap<String, List<InputVariant>> cappedGrouped = new LinkedHashMap<>();
        for (Map.Entry<String, List<InputVariant>> entry : grouped.entrySet()) {
            List<InputVariant> sortedForSaga = new ArrayList<>(entry.getValue());
            sortedForSaga.sort(Comparator
                    .comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(InputVariant::sourceClassFqn, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(InputVariant::sourceMethodName, Comparator.nullsFirst(String::compareTo))
                    .thenComparing(InputVariant::sourceBindingName, Comparator.nullsFirst(String::compareTo)));

            if (sortedForSaga.size() > maxPerSaga) {
                capped += sortedForSaga.size() - maxPerSaga;
                warnings.add("capped input variants for saga " + entry.getKey()
                        + " at maxInputVariantsPerSaga=" + maxPerSaga);
                sortedForSaga = sortedForSaga.subList(0, maxPerSaga);
            }

            cappedGrouped.put(entry.getKey(), List.copyOf(sortedForSaga));
        }

        List<InputVariant> flattened = cappedGrouped.values().stream()
                .flatMap(List::stream)
                .toList();
        rejected.sort(Comparator
                .comparing((RejectedInputVariant rejectedInput) -> rejectedInput.inputVariant().deterministicId(), Comparator.nullsFirst(String::compareTo))
                .thenComparing(rejectedInput -> rejectedInput.rejectionReason() == null ? null : rejectedInput.rejectionReason().name(), Comparator.nullsFirst(String::compareTo)));

        counts.put("inputVariantsSeen", seen);
        counts.put("inputVariantsAccepted", flattened.size());
        counts.put("inputVariantsDeduplicated", deduplicated);
        counts.put("inputVariantsExcludedByPolicy", excludedByPolicy);
        counts.put("inputVariantsRejectedBySourceMode", rejectedBySourceMode);
        counts.put("inputVariantsCapped", capped);
        counts.put("inputSagasWithInputs", cappedGrouped.size());

        return new NormalizationResult(List.copyOf(flattened), Collections.unmodifiableMap(cappedGrouped), List.copyOf(rejected), Collections.unmodifiableMap(counts), List.copyOf(warnings));
    }

    private static InputVariant normalizeVariant(InputVariant input) {
        List<String> constructorArgs = normalizeStrings(input.constructorArgumentSummaries());
        Map<String, String> logicalBindings = normalizeMap(input.logicalKeyBindings());
        List<String> warnings = normalizeStrings(input.warnings());

        InputVariant normalized = new InputVariant(
                null,
                normalize(input.sagaFqn()),
                normalize(input.sourceClassFqn()),
                normalize(input.sourceMethodName()),
                normalize(input.sourceBindingName()),
                input.resolutionStatus(),
                input.sourceMode(),
                input.sourceModeConfidence(),
                input.sourceModeEvidence(),
                normalize(input.stableSourceText()),
                normalize(input.provenanceText()),
                constructorArgs,
                logicalBindings,
                warnings);

        String deterministicId = ScenarioIdGenerator.inputVariantId(
                normalized.sagaFqn(),
                normalized.sourceClassFqn(),
                normalized.sourceMethodName(),
                normalized.sourceBindingName(),
                normalized.resolutionStatus(),
                normalized.stableSourceText(),
                normalized.provenanceText(),
                normalized.constructorArgumentSummaries(),
                normalized.logicalKeyBindings());

        return new InputVariant(
                deterministicId,
                normalized.sagaFqn(),
                normalized.sourceClassFqn(),
                normalized.sourceMethodName(),
                normalized.sourceBindingName(),
                normalized.resolutionStatus(),
                normalized.sourceMode(),
                normalized.sourceModeConfidence(),
                normalized.sourceModeEvidence(),
                normalized.stableSourceText(),
                normalized.provenanceText(),
                normalized.constructorArgumentSummaries(),
                normalized.logicalKeyBindings(),
                normalized.warnings());
    }

    private static SourceModeRejectionReason sourceModeRejectionReason(SourceMode sourceMode) {
        SourceMode safeMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        return switch (safeMode) {
            case TCC -> SourceModeRejectionReason.SOURCE_MODE_TCC_REJECTED_FOR_SAGA_CATALOG;
            case MIXED -> SourceModeRejectionReason.SOURCE_MODE_MIXED_REJECTED_FOR_SAGA_CATALOG;
            case SAGAS, UNKNOWN -> null;
        };
    }

    private static InputVariant mergeAcceptedInputs(InputVariant left, InputVariant right) {
        InputVariant sourceModeSource = chooseSourceModeSource(left, right);
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>();
        mergedWarnings.addAll(removeUnknownSourceModeWarning(left.warnings()));
        mergedWarnings.addAll(removeUnknownSourceModeWarning(right.warnings()));
        return new InputVariant(
                left.deterministicId(),
                left.sagaFqn(),
                left.sourceClassFqn(),
                left.sourceMethodName(),
                left.sourceBindingName(),
                left.resolutionStatus(),
                sourceModeSource.sourceMode(),
                sourceModeSource.sourceModeConfidence(),
                sourceModeSource.sourceModeEvidence(),
                left.stableSourceText(),
                left.provenanceText(),
                left.constructorArgumentSummaries(),
                left.logicalKeyBindings(),
                List.copyOf(mergedWarnings));
    }

    private static InputVariant chooseSourceModeSource(InputVariant left, InputVariant right) {
        if (left.sourceMode() == SourceMode.UNKNOWN && right.sourceMode() != SourceMode.UNKNOWN) {
            return right;
        }
        return left;
    }

    private static List<String> removeUnknownSourceModeWarning(List<String> warnings) {
        return warnings.stream()
                .filter(warning -> !"Source mode could not be proven; accepted by default unknown-mode policy.".equals(warning))
                .toList();
    }

    private static InputVariant addUnknownSourceModeWarning(InputVariant input) {
        if (input.sourceMode() != SourceMode.UNKNOWN) {
            return input;
        }
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>(input.warnings());
        mergedWarnings.add("Source mode could not be proven; accepted by default unknown-mode policy.");
        return new InputVariant(
                input.deterministicId(),
                input.sagaFqn(),
                input.sourceClassFqn(),
                input.sourceMethodName(),
                input.sourceBindingName(),
                input.resolutionStatus(),
                input.sourceMode(),
                input.sourceModeConfidence(),
                input.sourceModeEvidence(),
                input.stableSourceText(),
                input.provenanceText(),
                input.constructorArgumentSummaries(),
                input.logicalKeyBindings(),
                List.copyOf(mergedWarnings));
    }

    private static boolean isAllowedByPolicy(InputResolutionStatus status, ScenarioGeneratorConfig.InputPolicy policy) {
        InputResolutionStatus safeStatus = status == null ? InputResolutionStatus.UNRESOLVED : status;
        ScenarioGeneratorConfig.InputPolicy safePolicy = policy == null
                ? ScenarioGeneratorConfig.InputPolicy.RESOLVED_OR_REPLAYABLE
                : policy;

        return switch (safePolicy) {
            case RESOLVED_ONLY -> safeStatus == InputResolutionStatus.RESOLVED;
            case RESOLVED_OR_REPLAYABLE -> safeStatus == InputResolutionStatus.RESOLVED
                    || safeStatus == InputResolutionStatus.REPLAYABLE;
            case ALLOW_PARTIAL -> safeStatus == InputResolutionStatus.RESOLVED
                    || safeStatus == InputResolutionStatus.REPLAYABLE
                    || safeStatus == InputResolutionStatus.PARTIAL;
            case ALLOW_UNRESOLVED -> true;
        };
    }

    private static InputVariant mergeWarnings(InputVariant left, InputVariant right) {
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>();
        mergedWarnings.addAll(left.warnings());
        mergedWarnings.addAll(right.warnings());

        return new InputVariant(
                left.deterministicId(),
                left.sagaFqn(),
                left.sourceClassFqn(),
                left.sourceMethodName(),
                left.sourceBindingName(),
                left.resolutionStatus(),
                left.sourceMode(),
                left.sourceModeConfidence(),
                left.sourceModeEvidence(),
                left.stableSourceText(),
                left.provenanceText(),
                left.constructorArgumentSummaries(),
                left.logicalKeyBindings(),
                List.copyOf(mergedWarnings));
    }

    private static List<String> normalizeStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }

        return values.stream()
                .map(InputVariantNormalizer::normalize)
                .filter(Objects::nonNull)
                .toList();
    }

    private static Map<String, String> normalizeMap(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return Map.of();
        }

        LinkedHashMap<String, String> normalized = new LinkedHashMap<>();
        values.entrySet().stream()
                .map(entry -> Map.entry(normalize(entry.getKey()), normalize(entry.getValue())))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> normalized.put(entry.getKey(), entry.getValue()));

        return normalized;
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public record NormalizationResult(
            List<InputVariant> inputs,
            Map<String, List<InputVariant>> inputsBySaga,
            List<RejectedInputVariant> rejectedInputVariants,
            Map<String, Integer> counts,
            List<String> warnings) {
    }
}
