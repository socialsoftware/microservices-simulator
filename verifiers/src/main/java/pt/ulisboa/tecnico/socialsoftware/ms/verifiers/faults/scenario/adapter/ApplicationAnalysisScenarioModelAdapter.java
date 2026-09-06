package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.AccessPolicy;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.DispatchPhase;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.EventConsequenceCandidate;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaFunctionalityBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.SagaStepBuildingBlock;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock.StepDispatchFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationEvidenceClass;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequenceDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventEmissionSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRecipeNode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputRole;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FixtureOrigin;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SourceSetupPlanBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.ApplicationAnalysisState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFacadeSetupActionTrace;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyFullTraceResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceOccurrence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovySourceValueReference;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyTraceArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueMetadata;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.GroovyValueResolutionCategory;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence;

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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ApplicationAnalysisScenarioModelAdapter {

    private static final Pattern JAVA_IDENTIFIER = Pattern.compile("[A-Za-z_$][A-Za-z\\d_$]*");

    private final InputRecipeMapper inputRecipeMapper = new InputRecipeMapper();

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

        List<EventConsequenceDefinition> eventDefinitions = adaptEventConsequences(state, diagnostics, counts);
        List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence = adaptAggregateKeyInputEvidence(
                state.sourceAggregateKeyInputEvidence(), adaptedInputs.adaptedTraces());
        List<SourceSetupPlanBinding> setupBindings = adaptSetupBindings(
                state, adaptedInputs.inputVariants(), adaptedInputs.adaptedTraces(), diagnostics, counts);
        LinkedHashMap<String, List<StepDispatchFootprint>> dispatchesBySaga = new LinkedHashMap<>();
        state.sagas.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SagaFunctionalityBuildingBlock::getFqn,
                        Comparator.nullsFirst(String::compareTo)))
                .forEach(saga -> dispatchesBySaga.put(saga.getFqn(), saga.getSteps().stream()
                        .filter(Objects::nonNull)
                        .flatMap(step -> step.getDispatches().stream())
                        .filter(Objects::nonNull)
                        .map(dispatch -> canonicalDispatchKey(saga.getFqn(), dispatch))
                        .toList()));
        return new ScenarioModelAdapterResult(sagaDefinitions, adaptedInputs.inputVariants(), eventDefinitions,
                setupBindings, counts, new ArrayList<>(diagnostics), dispatchesBySaga,
                aggregateKeyInputEvidence);
    }

    private List<SourceAggregateKeyInputEvidence> adaptAggregateKeyInputEvidence(
            List<SourceAggregateKeyInputEvidence> rawEvidence,
            List<AdaptedTrace> adaptedTraces) {
        LinkedHashSet<SourceAggregateKeyInputEvidence> result = new LinkedHashSet<>();
        for (SourceAggregateKeyInputEvidence evidence : rawEvidence == null
                ? List.<SourceAggregateKeyInputEvidence>of() : rawEvidence) {
            for (AdaptedTrace adaptedTrace : adaptedTraces == null ? List.<AdaptedTrace>of() : adaptedTraces) {
                if (!evidenceMatchesTrace(evidence, adaptedTrace.trace())) {
                    continue;
                }
                result.add(new SourceAggregateKeyInputEvidence(
                        evidence.sagaFqn(), evidence.sourceClassFqn(), evidence.sourceMethodName(),
                        evidence.callContextMethodName(), evidence.sourceBindingName(),
                        evidence.constructorArgumentIndex(), evidence.aggregateName(),
                        evidence.producerReference(), evidence.aggregateKeyPropertyPath(),
                        adaptedTrace.inputVariantId()));
            }
        }
        return result.stream()
                .sorted(Comparator
                        .comparing(SourceAggregateKeyInputEvidence::inputVariantId,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparing(SourceAggregateKeyInputEvidence::aggregateName,
                                Comparator.nullsFirst(String::compareTo))
                        .thenComparingInt(SourceAggregateKeyInputEvidence::constructorArgumentIndex)
                        .thenComparing(evidence -> String.join(".", evidence.aggregateKeyPropertyPath())))
                .toList();
    }

    private boolean evidenceMatchesTrace(SourceAggregateKeyInputEvidence evidence,
                                         GroovyFullTraceResult trace) {
        if (evidence == null || trace == null
                || !Objects.equals(evidence.sagaFqn(), trace.sagaClassFqn())
                || !Objects.equals(evidence.sourceClassFqn(), trace.sourceClassFqn())
                || !Objects.equals(evidence.sourceMethodName(), trace.sourceMethodName())
                || !Objects.equals(evidence.callContextMethodName(), trace.callContextMethodName())
                || !Objects.equals(evidence.sourceBindingName(), trace.sourceBindingName())) {
            return false;
        }
        return trace.constructorArguments().stream()
                .filter(Objects::nonNull)
                .filter(argument -> argument.index() == evidence.constructorArgumentIndex())
                .map(GroovyTraceArgument::producerReference)
                .filter(Objects::nonNull)
                .map(reference -> {
                    GroovySourceValueReference resolved = reference;
                    for (String property : evidence.aggregateKeyPropertyPath()) {
                        resolved = resolved.appendProperty(property);
                    }
                    return resolved;
                })
                .anyMatch(reference -> Objects.equals(reference, evidence.producerReference()));
    }

    private StepDispatchFootprint canonicalDispatchKey(String sagaFqn, StepDispatchFootprint dispatch) {
        if (dispatch.stepKey() == null || sagaFqn == null) {
            return dispatch;
        }
        int separator = dispatch.stepKey().lastIndexOf("::");
        String stepName = separator < 0 ? dispatch.stepKey() : dispatch.stepKey().substring(separator + 2);
        String canonicalStepKey = sagaFqn + "::" + stepName;
        if (Objects.equals(canonicalStepKey, dispatch.stepKey())) {
            return dispatch;
        }
        return new StepDispatchFootprint(canonicalStepKey, dispatch.commandTypeFqn(), dispatch.aggregateName(),
                dispatch.accessPolicy(), dispatch.phase(), dispatch.multiplicity(), dispatch.aggregateKeyText(),
                dispatch.aggregateKeyConfidence(), dispatch.aggregateKeyConstructorArgumentIndex(),
                dispatch.aggregateKeyPropertyPath());
    }

    private List<SourceSetupPlanBinding> adaptSetupBindings(
            ApplicationAnalysisState state,
            List<InputVariant> inputs,
            List<AdaptedTrace> adaptedTraces,
            LinkedHashSet<String> diagnostics,
            LinkedHashMap<String, Integer> counts) {
        SetupPlanMapper mapper = new SetupPlanMapper();
        ScenarioExecutorReadinessEvaluator readinessEvaluator = new ScenarioExecutorReadinessEvaluator();
        List<SourceSetupPlanBinding> bindings = new ArrayList<>();
        Map<String, Set<String>> setupOnlyCoverageByClass = new LinkedHashMap<>();
        Set<String> setupSourceClasses = state.groovyFacadeSetupActionTraces.stream()
                .filter(trace -> "setup".equals(trace.callContextMethodName()))
                .map(trace -> trace.sourceClassFqn())
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, InputVariant> inputsById = inputs.stream()
                .collect(Collectors.toMap(InputVariant::deterministicId, input -> input,
                        (left, right) -> left, LinkedHashMap::new));
        for (String sourceClassFqn : setupSourceClasses.stream().sorted().toList()) {
            List<GroovyFacadeSetupActionTrace> traces =
                    state.groovyFacadeSetupActionTraces.stream()
                            .filter(trace -> Objects.equals(trace.sourceClassFqn(), sourceClassFqn))
                            .filter(trace -> "setup".equals(trace.callContextMethodName()))
                            .sorted(Comparator.comparingInt(trace -> trace.occurrence() == null
                                    ? Integer.MAX_VALUE : trace.occurrence().orderIndex()))
                            .toList();
            if (traces.isEmpty()) continue;

            Map<String, GroovyFacadeSetupActionTrace> setupActionByOccurrence = traces.stream()
                    .filter(trace -> trace.sourceOccurrence() != null)
                    .collect(Collectors.toMap(GroovyFacadeSetupActionTrace::sourceOccurrence,
                            trace -> trace, (left, right) -> left, LinkedHashMap::new));
            Map<String, List<AdaptedTrace>> nestedSetupTargetsByInputId = adaptedTraces.stream()
                    .filter(adaptedTrace -> Objects.equals(adaptedTrace.trace().sourceClassFqn(), sourceClassFqn))
                    .filter(adaptedTrace -> "setup".equals(adaptedTrace.trace().callContextMethodName()))
                    .filter(adaptedTrace -> hasNestedSourceReference(
                            adaptedTrace.trace().constructorArguments()))
                    .collect(Collectors.groupingBy(AdaptedTrace::inputVariantId,
                            LinkedHashMap::new, Collectors.toList()));
            Map<String, List<AdaptedTrace>> setupTargetsByInputId = nestedSetupTargetsByInputId.entrySet().stream()
                    .filter(entry -> entry.getValue().stream().allMatch(adaptedTrace ->
                            adaptedTrace.trace().occurrence() != null
                                    && setupActionByOccurrence.containsKey(
                                    adaptedTrace.trace().occurrence().occurrenceId())))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                            (left, right) -> left, LinkedHashMap::new));
            nestedSetupTargetsByInputId.entrySet().stream()
                    .filter(entry -> !setupTargetsByInputId.containsKey(entry.getKey()))
                    .forEach(entry -> diagnostics.add("blocked setup-derived target for "
                            + sourceClassFqn + " input " + entry.getKey()
                            + ": missing exact target occurrence metadata"));
            Set<String> setupTargetInputIds = nestedSetupTargetsByInputId.keySet();

            LinkedHashMap<String, SetupPlanMapper.ParticipantSource> eligibleParticipants = new LinkedHashMap<>();
            adaptedTraces.stream()
                    .filter(adaptedTrace -> Objects.equals(adaptedTrace.trace().sourceClassFqn(), sourceClassFqn))
                    .filter(adaptedTrace -> !setupTargetInputIds.contains(adaptedTrace.inputVariantId()))
                    .forEach(adaptedTrace -> {
                        GroovyFullTraceResult trace = adaptedTrace.trace();
                        InputVariant input = inputsById.get(adaptedTrace.inputVariantId());
                        if (input == null || eligibleParticipants.containsKey(input.deterministicId())) return;
                        SetupPlanMapper.ParticipantSource participant = new SetupPlanMapper.ParticipantSource(
                                input.deterministicId(), trace.constructorArguments());
                        if (eligibleForSetup(input, participant, traces, mapper, readinessEvaluator,
                                diagnostics, sourceClassFqn)) {
                            eligibleParticipants.put(input.deterministicId(), participant);
                        }
                    });
            LinkedHashSet<String> coveredInputIds = new LinkedHashSet<>();
            if (!eligibleParticipants.isEmpty()) {
                List<SetupPlanMapper.ParticipantSource> participants = eligibleParticipants.values().stream()
                        .sorted(Comparator.comparing(SetupPlanMapper.ParticipantSource::inputVariantId))
                        .toList();
                var plan = mapper.map(traces, participants);
                SetupPlanValidator.ValidationResult validation = new SetupPlanValidator().validate(plan);
                if (!validation.valid()) {
                    diagnostics.add("blocked source setup for " + sourceClassFqn + ": " + validation.diagnostics());
                } else {
                    List<String> inputVariantIds = participants.stream()
                            .map(SetupPlanMapper.ParticipantSource::inputVariantId)
                            .toList();
                    bindings.add(new SourceSetupPlanBinding(inputVariantIds, plan));
                    coveredInputIds.addAll(inputVariantIds);
                }
            }

            LinkedHashMap<String, Integer> setupActionOrders = new LinkedHashMap<>();
            traces.stream().filter(trace -> trace.occurrence() != null).forEach(trace ->
                    setupActionOrders.put(trace.sourceOccurrence(), trace.occurrence().orderIndex()));
            setupTargetsByInputId.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
                List<AdaptedTrace> distinctTargets = entry.getValue().stream()
                        .collect(Collectors.toMap(
                                target -> target.trace().occurrence().occurrenceId(),
                                target -> target,
                                (left, right) -> left,
                                LinkedHashMap::new))
                        .values().stream().toList();
                if (distinctTargets.size() != 1) {
                    diagnostics.add("blocked setup-derived target for " + sourceClassFqn + " input "
                            + entry.getKey() + ": ambiguous target source occurrences "
                            + distinctTargets.stream().map(target -> target.trace().occurrence().occurrenceId())
                            .sorted().toList());
                    return;
                }
                AdaptedTrace target = distinctTargets.getFirst();
                String targetOccurrence = target.trace().occurrence().occurrenceId();
                int targetOrder = target.trace().occurrence().orderIndex();
                List<GroovyFacadeSetupActionTrace> prefix = traces.stream()
                        .filter(trace -> trace.occurrence() != null)
                        .filter(trace -> trace.occurrence().orderIndex() < targetOrder)
                        .toList();
                if (prefix.isEmpty()) return;
                InputVariant input = inputsById.get(entry.getKey());
                if (input == null) return;
                SetupPlanMapper.ParticipantSource participant = new SetupPlanMapper.ParticipantSource(
                        input.deterministicId(), target.trace().constructorArguments());
                if (!eligibleForSetup(input, participant, prefix, mapper, readinessEvaluator,
                        diagnostics, sourceClassFqn + "#setup-before-" + targetOccurrence)) {
                    return;
                }
                var plan = mapper.map(prefix, List.of(participant));
                SetupPlanValidator.ValidationResult validation = new SetupPlanValidator().validate(plan);
                if (!validation.valid()) {
                    diagnostics.add("blocked setup-derived target for " + sourceClassFqn + " input "
                            + input.deterministicId() + ": " + validation.diagnostics());
                    return;
                }
                bindings.add(new SourceSetupPlanBinding(List.of(input.deterministicId()), plan,
                        sourceClassFqn, "setup", Map.of(input.deterministicId(), List.of(targetOccurrence)),
                        targetOccurrence, Map.of(input.deterministicId(), targetOrder), setupActionOrders));
                coveredInputIds.add(input.deterministicId());
            });
            if (!coveredInputIds.isEmpty()) {
                setupOnlyCoverageByClass.put(sourceClassFqn, Set.copyOf(coveredInputIds));
            }
        }

        Map<FeatureContext, List<AdaptedTrace>> featureTraces = adaptedTraces.stream()
                .filter(adaptedTrace -> adaptedTrace.trace().occurrence() != null)
                .filter(adaptedTrace -> isFeatureContext(adaptedTrace.trace().callContextMethodName()))
                .collect(Collectors.groupingBy(adaptedTrace -> new FeatureContext(
                                adaptedTrace.trace().sourceClassFqn(),
                                adaptedTrace.trace().callContextMethodName()),
                        LinkedHashMap::new, Collectors.toList()));
        featureTraces.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> adaptFeatureSetupBindings(entry.getKey(), entry.getValue(), state,
                        inputsById, setupOnlyCoverageByClass, mapper, readinessEvaluator,
                        diagnostics, bindings));

        counts.put("sourceSetupPlanBindings", bindings.size());
        if (!setupSourceClasses.isEmpty() && bindings.isEmpty()) {
            diagnostics.add("observed setup contexts had no extractable straight-line setup plan");
        }
        return bindings.stream().distinct().toList();
    }

    private boolean hasNestedSourceReference(List<GroovyTraceArgument> arguments) {
        return arguments != null && arguments.stream().filter(Objects::nonNull)
                .filter(argument -> argument.producerReference() == null)
                .map(GroovyTraceArgument::recipe)
                .anyMatch(recipe -> recipe != null && recipe.sourceReference() == null
                        && containsSourceReferenceBelowRoot(recipe));
    }

    private boolean containsSourceReferenceBelowRoot(GroovyValueRecipe recipe) {
        if (recipe == null) return false;
        if (recipe.sourceReference() != null) return true;
        if (recipe.children().stream().anyMatch(this::containsSourceReferenceBelowRoot)) return true;
        return recipe.metadata() != null && recipe.metadata().assignments().stream()
                .filter(Objects::nonNull)
                .map(assignment -> assignment.valueRecipe())
                .anyMatch(this::containsSourceReferenceBelowRoot);
    }

    private void adaptFeatureSetupBindings(
            FeatureContext context,
            List<AdaptedTrace> adaptedTraces,
            ApplicationAnalysisState state,
            Map<String, InputVariant> inputsById,
            Map<String, Set<String>> setupOnlyCoverageByClass,
            SetupPlanMapper mapper,
            ScenarioExecutorReadinessEvaluator readinessEvaluator,
            LinkedHashSet<String> diagnostics,
            List<SourceSetupPlanBinding> bindings) {
        Map<String, List<AdaptedTrace>> tracesByInput = adaptedTraces.stream()
                .collect(Collectors.groupingBy(AdaptedTrace::inputVariantId,
                        LinkedHashMap::new, Collectors.toList()));
        LinkedHashMap<String, AdaptedTrace> uniqueTraces = new LinkedHashMap<>();
        tracesByInput.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(entry -> {
            List<AdaptedTrace> distinct = entry.getValue().stream()
                    .filter(trace -> trace.trace().occurrence() != null)
                    .collect(Collectors.toMap(
                            trace -> trace.trace().occurrence().occurrenceId(),
                            trace -> trace,
                            (left, right) -> left,
                            LinkedHashMap::new))
                    .values().stream().toList();
            if (distinct.size() == 1) {
                uniqueTraces.put(entry.getKey(), distinct.get(0));
            } else if (distinct.size() > 1) {
                diagnostics.add("blocked feature-derived setup for " + context.sourceClassFqn()
                        + "#" + context.featureMethodName() + " input " + entry.getKey()
                        + ": ambiguous target source occurrences " + distinct.stream()
                        .map(trace -> trace.trace().occurrence().occurrenceId()).sorted().toList());
            }
        });
        if (uniqueTraces.isEmpty()) return;

        List<GroovyFacadeSetupActionTrace> setupActions = state.groovyFacadeSetupActionTraces.stream()
                .filter(trace -> Objects.equals(trace.sourceClassFqn(), context.sourceClassFqn()))
                .filter(trace -> "setup".equals(trace.callContextMethodName()))
                .sorted(Comparator.comparingInt(trace -> trace.occurrence() == null
                        ? Integer.MAX_VALUE : trace.occurrence().orderIndex()))
                .toList();
        List<GroovyFacadeSetupActionTrace> featureActions = state.groovyFacadeSetupActionTraces.stream()
                .filter(trace -> Objects.equals(trace.sourceClassFqn(), context.sourceClassFqn()))
                .filter(trace -> Objects.equals(trace.callContextMethodName(), context.featureMethodName()))
                .filter(trace -> trace.occurrence() != null && trace.occurrence().initialPreparationPhase())
                .sorted(Comparator.comparingInt(trace -> trace.occurrence().orderIndex()))
                .toList();

        uniqueTraces.values().stream()
                .map(AdaptedTrace::trace)
                .map(GroovyFullTraceResult::occurrence)
                .filter(GroovySourceOccurrence::initialPreparationPhase)
                .sorted(Comparator.comparingInt(GroovySourceOccurrence::orderIndex))
                .distinct()
                .forEach(frontier -> {
                    List<GroovyFacadeSetupActionTrace> actions = new ArrayList<>(setupActions);
                    featureActions.stream()
                            .filter(action -> action.occurrence().orderIndex() < frontier.orderIndex())
                            .forEach(actions::add);
                    if (actions.isEmpty()) return;

                    LinkedHashMap<String, SetupPlanMapper.ParticipantSource> eligible = new LinkedHashMap<>();
                    uniqueTraces.entrySet().stream()
                            .filter(candidate -> candidate.getValue().trace().occurrence().initialPreparationPhase())
                            .filter(candidate -> candidate.getValue().trace().occurrence().orderIndex()
                                    >= frontier.orderIndex())
                            .forEach(candidate -> {
                                InputVariant input = inputsById.get(candidate.getKey());
                                if (input == null) return;
                                SetupPlanMapper.ParticipantSource participant = new SetupPlanMapper.ParticipantSource(
                                        input.deterministicId(), candidate.getValue().trace().constructorArguments());
                                if (eligibleForSetup(input, participant, actions, mapper, readinessEvaluator,
                                        diagnostics, context.sourceClassFqn() + "#" + context.featureMethodName())) {
                                    eligible.put(input.deterministicId(), participant);
                                }
                            });
                    if (eligible.isEmpty()
                            || setupOnlyCoverageByClass.getOrDefault(context.sourceClassFqn(), Set.of())
                            .containsAll(eligible.keySet())) {
                        return;
                    }

                    List<SetupPlanMapper.ParticipantSource> participants = eligible.values().stream()
                            .sorted(Comparator.comparing(SetupPlanMapper.ParticipantSource::inputVariantId))
                            .toList();
                    var plan = mapper.map(actions, participants);
                    if (!new SetupPlanValidator().validate(plan).valid()) return;
                    LinkedHashMap<String, List<String>> targets = new LinkedHashMap<>();
                    LinkedHashMap<String, Integer> targetOrders = new LinkedHashMap<>();
                    eligible.keySet().forEach(inputId -> targets.put(inputId, List.of(
                            uniqueTraces.get(inputId).trace().occurrence().occurrenceId())));
                    eligible.keySet().forEach(inputId -> targetOrders.put(inputId,
                            uniqueTraces.get(inputId).trace().occurrence().orderIndex()));
                    LinkedHashMap<String, Integer> featureActionOrders = new LinkedHashMap<>();
                    featureActions.forEach(action -> featureActionOrders.put(
                            action.occurrence().occurrenceId(), action.occurrence().orderIndex()));
                    bindings.add(new SourceSetupPlanBinding(eligible.keySet().stream().sorted().toList(),
                            plan, context.sourceClassFqn(), context.featureMethodName(), targets,
                            frontier.occurrenceId(), targetOrders, featureActionOrders));
                });

        uniqueTraces.values().stream()
                .filter(trace -> !trace.trace().occurrence().initialPreparationPhase())
                .forEach(trace -> diagnostics.add("blocked feature-derived setup for "
                        + context.sourceClassFqn() + "#" + context.featureMethodName() + " input "
                        + trace.inputVariantId() + ": target follows assertion, control-flow, or workflow barrier"));
    }

    private boolean eligibleForSetup(InputVariant input,
                                     SetupPlanMapper.ParticipantSource participant,
                                     List<GroovyFacadeSetupActionTrace> actions,
                                     SetupPlanMapper mapper,
                                     ScenarioExecutorReadinessEvaluator readinessEvaluator,
                                     LinkedHashSet<String> diagnostics,
                                     String context) {
        var participantPlan = mapper.map(actions, List.of(participant));
        Set<Integer> boundArguments = participantPlan.participantBindings().stream()
                .filter(binding -> Objects.equals(binding.inputVariantId(), input.deterministicId()))
                .map(binding -> binding.argumentIndex())
                .collect(Collectors.toSet());
        boolean independentlyReady = input.inputRecipe() != null
                && input.inputRecipe().arguments().stream()
                .filter(argument -> !boundArguments.contains(argument.index()))
                .allMatch(argument -> readinessEvaluator.evaluate(argument).materializable());
        if (boundArguments.isEmpty() && !independentlyReady) return false;
        SetupPlanValidator.ValidationResult validation = new SetupPlanValidator().validate(participantPlan);
        if (!validation.valid()) {
            diagnostics.add("blocked source setup for " + context + " input "
                    + input.deterministicId() + ": " + validation.diagnostics());
            return false;
        }
        if (!independentlyReady) {
            diagnostics.add("blocked source setup for " + context + " input "
                    + input.deterministicId() + ": incomplete setup-dependent argument coverage");
            return false;
        }
        return true;
    }

    private boolean isFeatureContext(String methodName) {
        return methodName != null && !"setup".equals(methodName) && !"setupSpec".equals(methodName)
                && !methodName.startsWith("field:") && !JAVA_IDENTIFIER.matcher(methodName).matches();
    }

    private List<EventConsequenceDefinition> adaptEventConsequences(ApplicationAnalysisState state,
                                                                      LinkedHashSet<String> diagnostics,
                                                                      LinkedHashMap<String, Integer> counts) {
        state.eventConsequenceDiagnostics.stream().sorted().forEach(diagnostics::add);
        List<EventConsequenceDefinition> definitions = state.eventConsequenceCandidates.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(EventConsequenceCandidate::triggerSagaFqn)
                        .thenComparing(EventConsequenceCandidate::triggerStepKey)
                        .thenComparing(candidate -> candidate.emissionSite().eventTypeFqn())
                        .thenComparing(candidate -> candidate.selectedConsumerRoute().eventHandlingClassFqn())
                        .thenComparing(candidate -> candidate.selectedConsumerRoute().eventHandlingMethodName()))
                .map(candidate -> {
                    String emissionId = ScenarioIdGenerator.eventEmissionSiteId(
                            candidate.emissionSite().sourceServiceClassFqn(),
                            candidate.emissionSite().sourceServiceMethodSignature(),
                            candidate.emissionSite().emissionOrdinal(),
                            candidate.emissionSite().eventTypeFqn());
                    EventEmissionSite site = new EventEmissionSite(
                            emissionId,
                            candidate.emissionSite().sourceServiceClassFqn(),
                            candidate.emissionSite().sourceServiceMethodSignature(),
                            candidate.emissionSite().emissionOrdinal(),
                            candidate.emissionSite().eventTypeFqn(),
                            candidate.emissionSite().extractionEvidence());
                    var route = candidate.selectedConsumerRoute();
                    return new EventConsequenceDefinition(
                            candidate.triggerSagaFqn(), candidate.triggerStepKey(), site,
                            route.eventHandlingClassFqn(), route.eventHandlingMethodName(),
                            route.eventHandlerClassFqn(), route.eventProcessingClassFqn(),
                            route.eventProcessingMethodName(), route.facadeClassFqn(),
                            route.facadeMethodName(), route.sagaClassFqn(),
                            EventConsequenceDefinition.UNIQUE_MATCHING_SUBSCRIBER,
                            candidate.diagnostics());
                })
                .toList();
        counts.put("eventConsequenceCandidatesAdapted", definitions.size());
        counts.put("eventConsequenceDiagnostics", state.eventConsequenceDiagnostics.size());
        return definitions;
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
        List<StepFootprint> forwardFootprints = new ArrayList<>();
        List<StepFootprint> compensationFootprints = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int typeOnlyFootprints = 0;

        for (StepDispatchFootprint dispatch : stepBlock.getDispatches() == null ? List.<StepDispatchFootprint>of() : stepBlock.getDispatches()) {
            StepFootprint footprint = adaptFootprint(sagaFqn, stepName, dispatch, warnings, diagnostics);
            if (dispatch.phase() == DispatchPhase.COMPENSATION) {
                compensationFootprints.add(footprint);
            } else {
                forwardFootprints.add(footprint);
            }
            if (footprint.aggregateKey() != null && footprint.aggregateKey().confidence() == FootprintConfidence.TYPE_ONLY) {
                typeOnlyFootprints++;
            }
        }

        List<String> analysisDiagnostics = stepBlock.getAnalysisDiagnostics().stream()
                .map(diagnostic -> diagnostic.phase() + ":" + diagnostic.code() + ": " + diagnostic.message())
                .sorted()
                .toList();
        analysisDiagnostics.forEach(diagnostic ->
                diagnostics.add(buildStepDiagnostic(sagaFqn, stepName, diagnostic)));

        if (stepName == null) {
            warnings.add("step with missing name was adapted conservatively");
        }

        CompensationEvidenceClass compensationEvidence = classifyCompensationEvidence(
                stepBlock.isCompensationRegistered(),
                stepBlock.isDispatchAnalysisComplete(DispatchPhase.FORWARD),
                forwardFootprints);

        return new StepAdaptation(
                new StepDefinition(
                        deterministicId,
                        stepKey,
                        stepName,
                        orderIndex,
                        predecessorStepKeys,
                        forwardFootprints,
                        compensationFootprints,
                        stepBlock.isCompensationRegistered(),
                        stepBlock.isDispatchAnalysisComplete(DispatchPhase.FORWARD),
                        stepBlock.isDispatchAnalysisComplete(DispatchPhase.COMPENSATION),
                        compensationEvidence,
                        analysisDiagnostics,
                        warnings),
                forwardFootprints.size() + compensationFootprints.size(),
                typeOnlyFootprints,
                warnings);
    }

    private CompensationEvidenceClass classifyCompensationEvidence(boolean compensationRegistered,
                                                                     boolean forwardAnalysisComplete,
                                                                     List<StepFootprint> forwardFootprints) {
        if (compensationRegistered) {
            return CompensationEvidenceClass.EXPLICIT_COMPENSATION;
        }
        if (forwardFootprints.stream().anyMatch(footprint -> footprint.accessMode() == AccessMode.WRITE)) {
            return CompensationEvidenceClass.IMPLICIT_SAGA_ROLLBACK;
        }
        boolean confidentlyEffectFree = forwardAnalysisComplete
                && !forwardFootprints.isEmpty()
                && forwardFootprints.stream().allMatch(footprint -> footprint.accessMode() == AccessMode.READ);
        return confidentlyEffectFree ? null : CompensationEvidenceClass.CONSERVATIVE_UNKNOWN;
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

        AggregateKey aggregateKey;
        if (dispatch.aggregateKeyText() == null || dispatch.aggregateKeyText().isBlank()) {
            aggregateKey = new AggregateKey(null, aggregateName, null, FootprintConfidence.TYPE_ONLY);
            footprintWarnings.add("type-only footprint for " + aggregateName);
            diagnostics.add(buildStepDiagnostic(sagaFqn, stepName, "type-only footprint for " + aggregateName));
        } else {
            aggregateKey = new AggregateKey(null, aggregateName, dispatch.aggregateKeyText(),
                    toFootprintConfidence(dispatch.aggregateKeyConfidence()),
                    dispatch.aggregateKeyConstructorArgumentIndex(), dispatch.aggregateKeyPropertyPath());
        }

        warnings.addAll(footprintWarnings);
        return new StepFootprint(aggregateKey, accessMode, footprintWarnings);
    }

    private FootprintConfidence toFootprintConfidence(StepDispatchFootprint.AggregateKeyConfidence confidence) {
        if (confidence == StepDispatchFootprint.AggregateKeyConfidence.EXACT) {
            return FootprintConfidence.EXACT;
        }
        return FootprintConfidence.SYMBOLIC;
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
        Map<String, List<InputOwner>> featureOwnersByClass = featureOwnersByClass(traces);

        LinkedHashMap<String, InputVariant> variantsBySagaAndId = new LinkedHashMap<>();
        List<AdaptedTrace> adaptedTraces = new ArrayList<>();
        int skippedCount = 0;
        int duplicateCount = 0;
        int partialTraceCount = 0;
        int unresolvedTraceCount = 0;
        int replayableTraceCount = 0;

        for (GroovyFullTraceResult trace : traces) {
            TraceAdaptation traceAdaptation = adaptTrace(trace, sagaDefinitionsByFqn, featureOwnersByClass);
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
            adaptedTraces.add(new AdaptedTrace(trace, traceAdaptation.variant().deterministicId()));
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

        return new AdaptedInputs(inputVariants, List.copyOf(adaptedTraces), traces.size(), skippedCount,
                duplicateCount, partialTraceCount, unresolvedTraceCount, replayableTraceCount,
                counts.getOrDefault("stepsSeen", 0), counts.getOrDefault("footprintsSeen", 0));
    }

    private TraceAdaptation adaptTrace(GroovyFullTraceResult trace,
                                       Map<String, SagaDefinition> sagaDefinitionsByFqn,
                                       Map<String, List<InputOwner>> featureOwnersByClass) {
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

        InputRecipe inputRecipe = inputRecipeMapper.map(constructorArguments, status);

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
                normalize(trace.callContextMethodName()),
                inputRole(trace, sourceMethodName),
                fixtureOrigin(trace, sourceMethodName),
                status,
                trace.sourceMode(),
                trace.sourceModeConfidence(),
                trace.sourceModeEvidence(),
                stableSourceText,
                provenanceText,
                ownersForTrace(sourceClassFqn, sourceMethodName, normalize(trace.callContextMethodName()), featureOwnersByClass),
                summaries,
                extractLogicalKeyBindings(inputRecipe),
                warnings,
                inputRecipe);

        String deterministicId = ScenarioIdGenerator.inputVariantId(
                variant.sagaFqn(),
                variant.sourceClassFqn(),
                variant.sourceMethodName(),
                variant.sourceBindingName(),
                variant.resolutionStatus(),
                variant.stableSourceText(),
                variant.provenanceText(),
                variant.constructorArgumentSummaries(),
                variant.logicalKeyBindings(),
                variant.inputRecipe() == null ? null : variant.inputRecipe().recipeFingerprint());

        return TraceAdaptation.usable(new InputVariant(
                deterministicId,
                variant.sagaFqn(),
                variant.sourceClassFqn(),
                variant.sourceMethodName(),
                variant.sourceBindingName(),
                variant.callContextMethodName(),
                variant.inputRole(),
                variant.fixtureOrigin(),
                variant.resolutionStatus(),
                variant.sourceMode(),
                variant.sourceModeConfidence(),
                variant.sourceModeEvidence(),
                variant.stableSourceText(),
                variant.provenanceText(),
                variant.owners(),
                variant.constructorArgumentSummaries(),
                variant.logicalKeyBindings(),
                variant.warnings(),
                variant.inputRecipe()),
                status,
                "trace " + traceDescriptor(trace) + " is " + status.name().toLowerCase(Locale.ROOT));
    }

    private Map<String, List<InputOwner>> featureOwnersByClass(List<GroovyFullTraceResult> traces) {
        LinkedHashMap<String, LinkedHashSet<InputOwner>> ownersByClass = new LinkedHashMap<>();
        for (GroovyFullTraceResult trace : traces) {
            String sourceClassFqn = normalize(trace.sourceClassFqn());
            String sourceMethodName = normalize(trace.sourceMethodName());
            String callContextMethodName = normalize(trace.callContextMethodName());
            String ownerMethodName = callContextMethodName == null ? sourceMethodName : callContextMethodName;
            if (sourceClassFqn == null || ownerMethodName == null || isFixtureContext(ownerMethodName) || JAVA_IDENTIFIER.matcher(ownerMethodName).matches()) {
                continue;
            }
            ownersByClass.computeIfAbsent(sourceClassFqn, ignored -> new LinkedHashSet<>())
                    .add(new InputOwner(sourceClassFqn, ownerMethodName));
        }
        LinkedHashMap<String, List<InputOwner>> result = new LinkedHashMap<>();
        ownersByClass.forEach((sourceClassFqn, owners) -> result.put(sourceClassFqn, List.copyOf(owners)));
        return Collections.unmodifiableMap(result);
    }

    private InputRole inputRole(GroovyFullTraceResult trace, String sourceMethodName) {
        return fixtureOrigin(trace, sourceMethodName) == FixtureOrigin.DIRECT_FEATURE
                ? InputRole.FEATURE_UNDER_TEST
                : InputRole.FIXTURE_PREREQUISITE;
    }

    private FixtureOrigin fixtureOrigin(GroovyFullTraceResult trace, String sourceMethodName) {
        String callContext = normalize(trace.callContextMethodName());
        if (sourceMethodName != null && sourceMethodName.startsWith("field:")) {
            return FixtureOrigin.FIELD;
        }
        if ("setupSpec".equals(sourceMethodName) || "setupSpec".equals(callContext)) {
            return FixtureOrigin.SETUP_SPEC;
        }
        if ("setup".equals(sourceMethodName)) {
            return FixtureOrigin.SETUP;
        }
        if ("setup".equals(callContext) && !Objects.equals(sourceMethodName, callContext)) {
            return FixtureOrigin.SETUP_HELPER;
        }
        return FixtureOrigin.DIRECT_FEATURE;
    }

    private Map<String, String> extractLogicalKeyBindings(InputRecipe inputRecipe) {
        if (inputRecipe == null || inputRecipe.arguments() == null || inputRecipe.arguments().isEmpty()) {
            return Map.of();
        }
        LinkedHashMap<String, String> bindings = new LinkedHashMap<>();
        inputRecipe.arguments().forEach(argument -> collectLogicalKeyBindings(argument.recipe(), bindings));
        return bindings.isEmpty() ? Map.of() : Collections.unmodifiableMap(bindings);
    }

    private void collectLogicalKeyBindings(InputRecipeNode node, LinkedHashMap<String, String> bindings) {
        if (node == null) {
            return;
        }
        for (InputRecipeAssignment assignment : node.assignments()) {
            String keyName = normalizeLogicalKeyName(assignment.propertyName());
            String keyValue = literalValue(assignment.valueRecipe());
            if (keyName != null && keyValue != null) {
                bindings.putIfAbsent(keyName, keyValue);
            }
            collectLogicalKeyBindings(assignment.valueRecipe(), bindings);
        }
        for (InputRecipeArgument argument : node.arguments()) {
            collectLogicalKeyBindings(argument.recipe(), bindings);
        }
        for (InputRecipeArgument argument : node.callArguments()) {
            collectLogicalKeyBindings(argument.recipe(), bindings);
        }
        for (InputRecipeNode element : node.elements()) {
            collectLogicalKeyBindings(element, bindings);
        }
        if (node.receiver() != null) {
            collectLogicalKeyBindings(node.receiver(), bindings);
        }
        if (node.resultRecipe() != null) {
            collectLogicalKeyBindings(node.resultRecipe(), bindings);
        }
    }

    private String normalizeLogicalKeyName(String propertyName) {
        String normalized = normalize(propertyName);
        if (normalized == null) {
            return null;
        }
        return switch (normalized) {
            case "aggregateId", "orderId", "id" -> normalized;
            default -> null;
        };
    }

    private String literalValue(InputRecipeNode node) {
        if (node == null || !"literal".equals(node.kind()) || node.value() == null) {
            return null;
        }
        return String.valueOf(node.value());
    }

    private List<InputOwner> ownersForTrace(String sourceClassFqn,
                                            String sourceMethodName,
                                            String callContextMethodName,
                                            Map<String, List<InputOwner>> featureOwnersByClass) {
        if (isFixtureContext(sourceMethodName) || isFixtureContext(callContextMethodName)) {
            return featureOwnersByClass.getOrDefault(sourceClassFqn, List.of());
        }
        if (sourceClassFqn == null || sourceMethodName == null) {
            return List.of();
        }
        if (callContextMethodName != null && !Objects.equals(sourceMethodName, callContextMethodName)) {
            return List.of(new InputOwner(sourceClassFqn, callContextMethodName));
        }
        return List.of(new InputOwner(sourceClassFqn, sourceMethodName));
    }

    private boolean isFixtureContext(String sourceMethodName) {
        return "setup".equals(sourceMethodName)
                || "setupSpec".equals(sourceMethodName)
                || (sourceMethodName != null && sourceMethodName.startsWith("field:"));
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
                left.callContextMethodName(),
                left.inputRole(),
                left.fixtureOrigin(),
                left.resolutionStatus(),
                sourceModeSource.sourceMode(),
                sourceModeSource.sourceModeConfidence(),
                sourceModeSource.sourceModeEvidence(),
                left.stableSourceText(),
                left.provenanceText(),
                mergeOwners(left, right),
                left.constructorArgumentSummaries(),
                left.logicalKeyBindings(),
                List.copyOf(mergedWarnings),
                left.inputRecipe());
    }

    private List<InputOwner> mergeOwners(InputVariant left, InputVariant right) {
        LinkedHashSet<InputOwner> owners = new LinkedHashSet<>();
        owners.addAll(left.owners());
        owners.addAll(right.owners());
        return List.copyOf(owners);
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
                || category == GroovyValueResolutionCategory.RUNTIME_CALL
                || category == GroovyValueResolutionCategory.EVENT_PLACEHOLDER;
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

    private record AdaptedTrace(GroovyFullTraceResult trace, String inputVariantId) {
    }

    private record FeatureContext(String sourceClassFqn, String featureMethodName)
            implements Comparable<FeatureContext> {
        @Override
        public int compareTo(FeatureContext other) {
            int sourceComparison = Comparator.nullsFirst(String::compareTo)
                    .compare(sourceClassFqn, other.sourceClassFqn);
            return sourceComparison != 0 ? sourceComparison
                    : Comparator.nullsFirst(String::compareTo)
                    .compare(featureMethodName, other.featureMethodName);
        }
    }

    private record AdaptedInputs(List<InputVariant> inputVariants,
                                 List<AdaptedTrace> adaptedTraces,
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
