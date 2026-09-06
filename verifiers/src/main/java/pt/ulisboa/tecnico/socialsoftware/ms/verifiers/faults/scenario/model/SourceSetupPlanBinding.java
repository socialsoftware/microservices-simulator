package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Set;
import java.util.LinkedHashSet;

/** Connects one coherent observed fixture setup to every accepted input it can supply. */
public record SourceSetupPlanBinding(
        List<String> inputVariantIds,
        SetupPlan setupPlan,
        String sourceClassFqn,
        String featureMethodName,
        Map<String, List<String>> targetOccurrencesByInputVariantId,
        String frontierOccurrenceId,
        Map<String, Integer> targetOrderByInputVariantId,
        Map<String, Integer> featureActionOrderByOccurrenceId) {

    public SourceSetupPlanBinding {
        inputVariantIds = inputVariantIds == null ? List.of() : inputVariantIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .sorted()
                .toList();
        List<String> validInputVariantIds = inputVariantIds;
        LinkedHashMap<String, List<String>> targets = new LinkedHashMap<>();
        if (targetOccurrencesByInputVariantId != null) {
            targetOccurrencesByInputVariantId.entrySet().stream()
                    .filter(entry -> entry.getKey() != null && validInputVariantIds.contains(entry.getKey()))
                    .sorted(Map.Entry.comparingByKey())
                    .forEach(entry -> targets.put(entry.getKey(), entry.getValue() == null ? List.of()
                            : entry.getValue().stream()
                            .filter(value -> value != null && !value.isBlank())
                            .distinct()
                            .sorted()
                            .toList()));
        }
        targetOccurrencesByInputVariantId = Map.copyOf(targets);
        targetOrderByInputVariantId = targetOrderByInputVariantId == null
                ? Map.of() : Map.copyOf(targetOrderByInputVariantId);
        featureActionOrderByOccurrenceId = featureActionOrderByOccurrenceId == null
                ? Map.of() : Map.copyOf(featureActionOrderByOccurrenceId);
    }

    public SourceSetupPlanBinding(List<String> inputVariantIds, SetupPlan setupPlan) {
        this(inputVariantIds, setupPlan, null, null, Map.of(), null, Map.of(), Map.of());
    }

    public boolean featureDerived() {
        return sourceClassFqn != null && featureMethodName != null;
    }

    public boolean replaysSelectedTarget(Set<String> selectedInputIds) {
        if (!featureDerived() || setupPlan == null || selectedInputIds == null || selectedInputIds.isEmpty()) {
            return false;
        }
        Set<String> selectedOccurrences = new LinkedHashSet<>();
        selectedInputIds.forEach(inputId -> selectedOccurrences.addAll(
                targetOccurrencesByInputVariantId.getOrDefault(inputId, List.of())));
        return setupPlan.actions().stream()
                .map(SetupAction::sourceOccurrence)
                .anyMatch(selectedOccurrences::contains);
    }

    public boolean matchesSelectedFrontier(Set<String> selectedInputIds) {
        if (!featureDerived()) return true;
        if (frontierOccurrenceId == null || selectedInputIds == null || selectedInputIds.isEmpty()
                || !targetOrderByInputVariantId.keySet().containsAll(selectedInputIds)) {
            return false;
        }
        if (selectedInputIds.stream().anyMatch(inputId ->
                targetOccurrencesByInputVariantId.getOrDefault(inputId, List.of()).size() != 1)) {
            return false;
        }
        int firstOrder = selectedInputIds.stream()
                .mapToInt(targetOrderByInputVariantId::get)
                .min().orElse(Integer.MAX_VALUE);
        int lastOrder = selectedInputIds.stream()
                .mapToInt(targetOrderByInputVariantId::get)
                .max().orElse(Integer.MIN_VALUE);
        Set<String> selectedOccurrences = new LinkedHashSet<>();
        selectedInputIds.forEach(inputId -> selectedOccurrences.addAll(
                targetOccurrencesByInputVariantId.getOrDefault(inputId, List.of())));
        if (!selectedOccurrences.contains(frontierOccurrenceId)
                || targetOrderByInputVariantId.entrySet().stream()
                .filter(entry -> selectedInputIds.contains(entry.getKey()))
                .filter(entry -> entry.getValue() == firstOrder)
                .map(entry -> targetOccurrencesByInputVariantId.getOrDefault(entry.getKey(), List.of()))
                .flatMap(List::stream)
                .noneMatch(frontierOccurrenceId::equals)) {
            return false;
        }
        return featureActionOrderByOccurrenceId.entrySet().stream()
                .filter(entry -> entry.getValue() >= firstOrder && entry.getValue() < lastOrder)
                .allMatch(entry -> selectedOccurrences.contains(entry.getKey()));
    }
}
