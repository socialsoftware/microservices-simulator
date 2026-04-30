package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ApplicationAnalysisScenarioModelAdapter {

    public ScenarioModelAdapterResult adapt(ApplicationAnalysisState state) {
        Objects.requireNonNull(state, "state");

        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        LinkedHashSet<String> diagnostics = new LinkedHashSet<>();

        List<SagaDefinition> sagaDefinitions = adaptSagas(state, diagnostics, counts);
        Map<String, SagaDefinition> sagaDefinitionsByFqn = sagaDefinitions.stream()
                .filter(saga -> saga.sagaFqn() != null)
                .collect(Collectors.toMap(SagaDefinition::sagaFqn, saga -> saga, (left, right) -> left, LinkedHashMap::new));

        AdaptedInputs adaptedInputs = adaptInputs(state, sagaDefinitionsByFqn, diagnostics, counts);

        Set<String> sagaFqns = sagaDefinitions.stream()
                .map(SagaDefinition::sagaFqn)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Set<String> sagaFqnsWithInputs = adaptedInputs.inputVariants().stream()
                .map(InputVariant::sagaFqn)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        for (String sagaFqn : sagaFqns) {
            if (!sagaFqnsWithInputs.contains(sagaFqn)) {
                String diagnostic = "saga " + sagaFqn + " has no usable input traces";
                diagnostics.add(diagnostic);
                counts.merge("sagasWithoutUsableInputs", 1, Integer::sum);
            }
        }

        counts.put("sagasSeen", sagaDefinitions.size());
        counts.put("stepsAdapted", adaptedInputs.stepCount());
        counts.put("footprintsAdapted", adaptedInputs.footprintCount());
        counts.putIfAbsent("typeOnlyFootprints", 0);
        counts.put("inputTracesSeen", adaptedInputs.inputTracesSeen());
        counts.put("inputVariantsAdapted", adaptedInputs.inputVariants().size());
        counts.put("inputVariantsDeduplicated", adaptedInputs.duplicateCount());
        counts.put("inputVariantsSkipped", adaptedInputs.skippedCount());
        counts.put("partialTraces", adaptedInputs.partialTraceCount());
        counts.put("unresolvedTraces", adaptedInputs.unresolvedTraceCount());
        counts.put("replayableTraces", adaptedInputs.replayableTraceCount());
        counts.putIfAbsent("sagasWithoutUsableInputs", 0);

        return new ScenarioModelAdapterResult(sagaDefinitions, adaptedInputs.inputVariants(), counts, new ArrayList<>(diagnostics));
    }

    private List<SagaDefinition> adaptSagas(ApplicationAnalysisState state,
                                            LinkedHashSet<String> diagnostics,
                                            LinkedHashMap<String, Integer> counts) {
        List<SagaFunctionalityBuildingBlock> sagaBlocks = state.sagas == null
                ? List.of()
                : state.sagas.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SagaFunctionalityBuildingBlock::getFqn, Comparator.nullsFirst(String::compareTo)))
                .toList();

        List<SagaDefinition> sagaDefinitions = new ArrayList<>();
        int totalSteps = 0;
        int totalFootprints = 0;
        int typeOnlyFootprints = 0;

        for (SagaFunctionalityBuildingBlock sagaBlock : sagaBlocks) {
            SagaAdaptation sagaAdaptation = adaptSaga(sagaBlock, diagnostics);
            sagaDefinitions.add(sagaAdaptation.sagaDefinition());
            totalSteps += sagaAdaptation.stepCount();
            totalFootprints += sagaAdaptation.footprintCount();
            typeOnlyFootprints += sagaAdaptation.typeOnlyFootprintCount();
        }

        counts.put("typeOnlyFootprints", typeOnlyFootprints);
        counts.put("sagaBlocksSeen", sagaBlocks.size());
        counts.put("sagasAdapted", sagaDefinitions.size());
        counts.put("stepsSeen", totalSteps);
        counts.put("footprintsSeen", totalFootprints);

        return List.copyOf(sagaDefinitions);
    }

    private SagaAdaptation adaptSaga(SagaFunctionalityBuildingBlock sagaBlock,
                                     LinkedHashSet<String> diagnostics) {
        String sagaFqn = normalize(sagaBlock.getFqn());
        List<SagaStepBuildingBlock> steps = sagaBlock.getSteps() == null
                ? List.of()
                : sagaBlock.getSteps().stream()
                .filter(Objects::nonNull)
                .toList();

        List<StepDefinition> adaptedSteps = new ArrayList<>();
        List<String> sagaWarnings = new ArrayList<>();
        int typeOnlyFootprints = 0;
        int footprintCount = 0;

        for (int index = 0; index < steps.size(); index++) {
            StepAdaptation stepAdaptation = adaptStep(sagaFqn, steps.get(index), index, diagnostics);
            adaptedSteps.add(stepAdaptation.stepDefinition());
            sagaWarnings.addAll(stepAdaptation.warnings());
            typeOnlyFootprints += stepAdaptation.typeOnlyFootprintCount();
            footprintCount += stepAdaptation.footprintCount();
        }

        if (adaptedSteps.isEmpty() && sagaFqn != null) {
            sagaWarnings.add("saga " + sagaFqn + " has no steps");
        }

        return new SagaAdaptation(
                new SagaDefinition(sagaFqn, adaptedSteps, sagaWarnings),
                adaptedSteps.size(),
                footprintCount,
                typeOnlyFootprints);
    }

    private StepAdaptation adaptStep(String sagaFqn,
                                     SagaStepBuildingBlock stepBlock,
                                     int orderIndex,
                                     LinkedHashSet<String> diagnostics) {
        String stepName = normalize(stepBlock.getName());
        String stepKey = sagaFqn == null || stepName == null ? null : sagaFqn + "::" + stepName;
        String deterministicId = stepKey == null ? null : stepKey + "#" + orderIndex;

        List<String> predecessorStepKeys = stepBlock.getPredecessorStepKeys() == null
                ? List.of()
                : List.copyOf(stepBlock.getPredecessorStepKeys());
        List<StepFootprint> footprints = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int typeOnlyFootprints = 0;

        for (StepDispatchFootprint dispatch : stepBlock.getDispatches() == null ? List.<StepDispatchFootprint>of() : stepBlock.getDispatches()) {
            StepFootprint footprint = adaptFootprint(sagaFqn, stepName, dispatch, warnings, diagnostics);
            footprints.add(footprint);
            if (footprint.aggregateKey() != null && footprint.aggregateKey().confidence() == FootprintConfidence.TYPE_ONLY) {
                typeOnlyFootprints++;
            }
        }

        if (stepName == null) {
            warnings.add("step with missing name was adapted conservatively");
        }

        return new StepAdaptation(
                new StepDefinition(deterministicId, stepKey, stepName, orderIndex, predecessorStepKeys, footprints, warnings),
                footprints.size(),
                typeOnlyFootprints,
                warnings);
    }

    private StepFootprint adaptFootprint(String sagaFqn,
                                         String stepName,
                                         StepDispatchFootprint dispatch,
                                         List<String> warnings,
                                         LinkedHashSet<String> diagnostics) {
        String aggregateName = normalize(dispatch.aggregateName());
        AccessMode accessMode = adaptAccessMode(dispatch.accessPolicy(), sagaFqn, stepName, aggregateName, warnings);
        List<String> footprintWarnings = new ArrayList<>();

        if (aggregateName == null) {
            String message = "skipped/unknown aggregate footprint because dispatch aggregate name is missing";
            footprintWarnings.add(message);
            diagnostics.add(buildStepDiagnostic(sagaFqn, stepName, message));
            warnings.addAll(footprintWarnings);
            return new StepFootprint(null, accessMode, footprintWarnings);
        }

        AggregateKey aggregateKey = new AggregateKey(null, aggregateName, null, FootprintConfidence.TYPE_ONLY);
        footprintWarnings.add("type-only footprint for " + aggregateName);
        diagnostics.add(buildStepDiagnostic(sagaFqn, stepName, "type-only footprint for " + aggregateName));

        warnings.addAll(footprintWarnings);
        return new StepFootprint(aggregateKey, accessMode, footprintWarnings);
    }

    private AccessMode adaptAccessMode(AccessPolicy accessPolicy,
                                       String sagaFqn,
                                       String stepName,
                                       String aggregateName,
                                       List<String> warnings) {
        if (accessPolicy == AccessPolicy.READ) {
            return AccessMode.READ;
        }

        if (accessPolicy == AccessPolicy.WRITE) {
            return AccessMode.WRITE;
        }

        String warning = "step " + defaultText(sagaFqn) + "::" + defaultText(stepName)
                + " defaults to conservative write access"
                + (aggregateName == null ? "" : " for " + aggregateName);
        warnings.add(warning);
        return AccessMode.WRITE;
    }

    private AdaptedInputs adaptInputs(ApplicationAnalysisState state,
                                      Map<String, SagaDefinition> sagaDefinitionsByFqn,
                                      LinkedHashSet<String> diagnostics,
                                      LinkedHashMap<String, Integer> counts) {
        List<GroovyFullTraceResult> traces = state.groovyFullTraceResults == null
                ? List.of()
                : state.groovyFullTraceResults.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(GroovyFullTraceResult::sourceClassFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(GroovyFullTraceResult::sourceMethodName, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(trace -> trace.sourceBindingName() == null ? "" : trace.sourceBindingName())
                        .thenComparing(GroovyFullTraceResult::sagaClassFqn, Comparator.nullsFirst(String::compareTo)))
                .toList();

        LinkedHashMap<String, InputVariant> variantsBySagaAndId = new LinkedHashMap<>();
        int skippedCount = 0;
        int duplicateCount = 0;
        int partialTraceCount = 0;
        int unresolvedTraceCount = 0;
        int replayableTraceCount = 0;

        for (GroovyFullTraceResult trace : traces) {
            TraceAdaptation traceAdaptation = adaptTrace(trace, sagaDefinitionsByFqn);
            if (!traceAdaptation.usable()) {
                skippedCount++;
                diagnostics.add(traceAdaptation.diagnostic());
                continue;
            }

            InputResolutionStatus status = traceAdaptation.status();
            if (status == InputResolutionStatus.PARTIAL) {
                partialTraceCount++;
                diagnostics.add(traceAdaptation.diagnostic());
            } else if (status == InputResolutionStatus.UNRESOLVED) {
                unresolvedTraceCount++;
                diagnostics.add(traceAdaptation.diagnostic());
            } else if (status == InputResolutionStatus.REPLAYABLE) {
                replayableTraceCount++;
            }

            String variantKey = traceAdaptation.variant().sagaFqn() + "|" + traceAdaptation.variant().deterministicId();
            InputVariant existing = variantsBySagaAndId.get(variantKey);
            if (existing == null) {
                variantsBySagaAndId.put(variantKey, traceAdaptation.variant());
            } else {
                duplicateCount++;
                diagnostics.add("deduplicated equivalent input variant " + traceAdaptation.variant().deterministicId()
                        + " for saga " + traceAdaptation.variant().sagaFqn());
                variantsBySagaAndId.put(variantKey, mergeWarnings(existing, traceAdaptation.variant()));
            }
        }

        List<InputVariant> inputVariants = variantsBySagaAndId.values().stream()
                .sorted(Comparator
                        .comparing(InputVariant::sagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();

        counts.put("inputTracesSeen", traces.size());
        counts.put("inputVariantsAdapted", inputVariants.size());
        counts.put("inputVariantsDeduplicated", duplicateCount);
        counts.put("inputVariantsSkipped", skippedCount);
        counts.put("partialTraces", partialTraceCount);
        counts.put("unresolvedTraces", unresolvedTraceCount);
        counts.put("replayableTraces", replayableTraceCount);

        return new AdaptedInputs(inputVariants, traces.size(), skippedCount, duplicateCount, partialTraceCount, unresolvedTraceCount, replayableTraceCount,
                counts.getOrDefault("stepsSeen", 0), counts.getOrDefault("footprintsSeen", 0));
    }

    private TraceAdaptation adaptTrace(GroovyFullTraceResult trace,
                                       Map<String, SagaDefinition> sagaDefinitionsByFqn) {
        String sagaFqn = normalize(trace.sagaClassFqn());
        String sourceClassFqn = normalize(trace.sourceClassFqn());
        String sourceMethodName = normalize(trace.sourceMethodName());
        String sourceBindingName = normalize(trace.sourceBindingName());
        List<GroovyTraceArgument> constructorArguments = trace.constructorArguments() == null
                ? List.of()
                : trace.constructorArguments().stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparingInt(GroovyTraceArgument::index))
                .toList();

        if (sagaFqn == null) {
            return TraceAdaptation.skipped(trace, "skipped trace " + traceDescriptor(trace) + ": missing saga FQN");
        }
        if (!sagaDefinitionsByFqn.containsKey(sagaFqn)) {
            return TraceAdaptation.skipped(trace, "skipped trace " + traceDescriptor(trace) + ": saga definition missing");
        }
        if (sourceClassFqn == null || sourceMethodName == null) {
            return TraceAdaptation.skipped(trace, "skipped trace " + traceDescriptor(trace) + ": missing source class or method");
        }
        if (constructorArguments.isEmpty()) {
            return TraceAdaptation.skipped(trace, "skipped trace " + traceDescriptor(trace) + ": no constructor arguments");
        }

        List<String> summaries = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        boolean hasResolved = false;
        boolean hasReplayable = false;
        boolean hasUnresolved = false;
        boolean hasPartial = false;

        for (GroovyTraceArgument argument : constructorArguments) {
            ValueEvidence evidence = summarizeValue(argument == null ? null : argument.recipe(),
                    Collections.newSetFromMap(new IdentityHashMap<GroovyValueRecipe, Boolean>()));
            InputResolutionStatus argumentStatus = toInputStatus(evidence);
            hasResolved |= argumentStatus == InputResolutionStatus.RESOLVED;
            hasReplayable |= argumentStatus == InputResolutionStatus.REPLAYABLE;
            hasUnresolved |= argumentStatus == InputResolutionStatus.UNRESOLVED;
            hasPartial |= argumentStatus == InputResolutionStatus.PARTIAL;

            String summary = summarizeArgument(argument, argumentStatus);
            summaries.add(summary);
            if (argumentStatus == InputResolutionStatus.PARTIAL || argumentStatus == InputResolutionStatus.UNRESOLVED) {
                warnings.add(summary);
            }
        }

        InputResolutionStatus status = classifyTraceStatus(hasResolved, hasReplayable, hasUnresolved, hasPartial);
        if (status == InputResolutionStatus.PARTIAL || status == InputResolutionStatus.UNRESOLVED) {
            warnings.add("trace " + traceDescriptor(trace) + " is " + status.name().toLowerCase(Locale.ROOT));
        }

        String stableSourceText = normalize(trace.sourceExpressionText());
        if (stableSourceText == null) {
            stableSourceText = traceDescriptor(trace);
        }

        String provenanceText = normalize(trace.traceText());
        if (provenanceText == null) {
            List<String> provenanceLines = new ArrayList<>();
            provenanceLines.addAll(nonBlank(trace.resolutionNotes()));
            provenanceLines.addAll(summaries);
            provenanceText = String.join(System.lineSeparator(), provenanceLines);
        }

        InputVariant variant = new InputVariant(
                null,
                sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                status,
                trace.sourceMode(),
                trace.sourceModeConfidence(),
                trace.sourceModeEvidence(),
                stableSourceText,
                provenanceText,
                summaries,
                Map.of(),
                warnings);

        String deterministicId = ScenarioIdGenerator.inputVariantId(
                variant.sagaFqn(),
                variant.sourceClassFqn(),
                variant.sourceMethodName(),
                variant.sourceBindingName(),
                variant.resolutionStatus(),
                variant.stableSourceText(),
                variant.provenanceText(),
                variant.constructorArgumentSummaries(),
                variant.logicalKeyBindings());

        return TraceAdaptation.usable(new InputVariant(
                deterministicId,
                variant.sagaFqn(),
                variant.sourceClassFqn(),
                variant.sourceMethodName(),
                variant.sourceBindingName(),
                variant.resolutionStatus(),
                variant.sourceMode(),
                variant.sourceModeConfidence(),
                variant.sourceModeEvidence(),
                variant.stableSourceText(),
                variant.provenanceText(),
                variant.constructorArgumentSummaries(),
                variant.logicalKeyBindings(),
                variant.warnings()),
                status,
                "trace " + traceDescriptor(trace) + " is " + status.name().toLowerCase(Locale.ROOT));
    }

    private InputVariant mergeWarnings(InputVariant left, InputVariant right) {
        LinkedHashSet<String> mergedWarnings = new LinkedHashSet<>();
        mergedWarnings.addAll(left.warnings());
        mergedWarnings.addAll(right.warnings());
        InputVariant sourceModeSource = left.sourceMode() == SourceMode.UNKNOWN && right.sourceMode() != SourceMode.UNKNOWN
                ? right
                : left;
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

    private ValueEvidence summarizeValue(GroovyValueRecipe recipe, Set<GroovyValueRecipe> visited) {
        if (recipe == null) {
            return new ValueEvidence(false, false, true);
        }

        if (!visited.add(recipe)) {
            return new ValueEvidence(false, false, true);
        }

        GroovyValueMetadata metadata = recipe.metadata();
        GroovyValueResolutionCategory category = metadata == null
                ? GroovyValueResolutionCategory.RESOLVED
                : metadata.category();

        boolean replayableCategory = category == GroovyValueResolutionCategory.SOURCE_PLACEHOLDER
                || category == GroovyValueResolutionCategory.INJECTABLE_PLACEHOLDER
                || category == GroovyValueResolutionCategory.RUNTIME_CALL;
        boolean unresolvedCategory = category == GroovyValueResolutionCategory.UNKNOWN_UNRESOLVED;
        boolean unresolvedKind = recipe.kind() == GroovyValueKind.UNRESOLVED_VARIABLE
                || recipe.kind() == GroovyValueKind.UNRESOLVED_RUNTIME_EDGE;

        boolean hasResolved = category == GroovyValueResolutionCategory.RESOLVED
                && !unresolvedKind;
        boolean hasReplayable = replayableCategory;
        boolean hasUnresolved = unresolvedCategory || (unresolvedKind && !replayableCategory);

        for (GroovyValueRecipe child : recipe.children()) {
            ValueEvidence childEvidence = summarizeValue(child, visited);
            hasResolved |= childEvidence.hasResolved();
            hasReplayable |= childEvidence.hasReplayable();
            hasUnresolved |= childEvidence.hasUnresolved();
        }

        return new ValueEvidence(hasResolved, hasReplayable, hasUnresolved);
    }

    private InputResolutionStatus toInputStatus(ValueEvidence evidence) {
        if (evidence.hasUnresolved() && (evidence.hasResolved() || evidence.hasReplayable())) {
            return InputResolutionStatus.PARTIAL;
        }

        if (evidence.hasUnresolved()) {
            return InputResolutionStatus.UNRESOLVED;
        }

        if (evidence.hasReplayable()) {
            return InputResolutionStatus.REPLAYABLE;
        }

        return InputResolutionStatus.RESOLVED;
    }

    private InputResolutionStatus classifyTraceStatus(boolean hasResolved,
                                                      boolean hasReplayable,
                                                      boolean hasUnresolved,
                                                      boolean hasPartial) {
        if (hasPartial) {
            return InputResolutionStatus.PARTIAL;
        }
        if (hasUnresolved && (hasResolved || hasReplayable)) {
            return InputResolutionStatus.PARTIAL;
        }
        if (hasUnresolved) {
            return InputResolutionStatus.UNRESOLVED;
        }
        if (hasReplayable) {
            return InputResolutionStatus.REPLAYABLE;
        }
        return InputResolutionStatus.RESOLVED;
    }

    private String summarizeArgument(GroovyTraceArgument argument, InputResolutionStatus status) {
        if (argument == null) {
            return "arg[?]: (missing) [unresolved]";
        }

        StringBuilder summary = new StringBuilder();
        summary.append("arg[").append(argument.index()).append("]: ");
        summary.append(defaultText(argument.provenance()));
        if (status != InputResolutionStatus.RESOLVED) {
            summary.append(" [").append(status.name().toLowerCase(Locale.ROOT)).append("]");
        }
        if (argument.expectedTypeFqn() != null && !argument.expectedTypeFqn().isBlank()) {
            summary.append(" (type=").append(argument.expectedTypeFqn()).append(")");
        }
        return summary.toString();
    }

    private String traceDescriptor(GroovyFullTraceResult trace) {
        return defaultText(trace.sourceClassFqn()) + "#" + defaultText(trace.sourceMethodName())
                + " -> " + defaultText(trace.sagaClassFqn())
                + (trace.sourceBindingName() == null || trace.sourceBindingName().isBlank()
                ? ""
                : " [binding=" + trace.sourceBindingName() + "]");
    }

    private String buildStepDiagnostic(String sagaFqn, String stepName, String message) {
        return "saga " + defaultText(sagaFqn) + " step " + defaultText(stepName) + ": " + message;
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String defaultText(String value) {
        return value == null || value.isBlank() ? "(unknown)" : value;
    }

    private List<String> nonBlank(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private record ValueEvidence(boolean hasResolved, boolean hasReplayable, boolean hasUnresolved) {
    }

    private record StepAdaptation(StepDefinition stepDefinition,
                                  int footprintCount,
                                  int typeOnlyFootprintCount,
                                  List<String> warnings) {
    }

    private record SagaAdaptation(SagaDefinition sagaDefinition,
                                  int stepCount,
                                  int footprintCount,
                                  int typeOnlyFootprintCount) {
    }

    private record TraceAdaptation(InputVariant variant,
                                   InputResolutionStatus status,
                                   boolean usable,
                                   String diagnostic) {

        private static TraceAdaptation skipped(GroovyFullTraceResult trace, String diagnostic) {
            return new TraceAdaptation(null, InputResolutionStatus.UNRESOLVED, false, diagnostic);
        }

        private static TraceAdaptation usable(InputVariant variant,
                                             InputResolutionStatus status,
                                             String diagnostic) {
            return new TraceAdaptation(variant, status, true, diagnostic);
        }
    }

    private record AdaptedInputs(List<InputVariant> inputVariants,
                                 int inputTracesSeen,
                                 int skippedCount,
                                 int duplicateCount,
                                 int partialTraceCount,
                                 int unresolvedTraceCount,
                                 int replayableTraceCount,
                                 int stepCount,
                                 int footprintCount) {
    }
}
