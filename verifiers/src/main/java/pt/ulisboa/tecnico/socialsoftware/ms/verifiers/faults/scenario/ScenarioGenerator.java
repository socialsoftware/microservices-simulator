package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConflictGraphBuilder.ConflictCandidate;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner.InputTuple;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScheduleEnumerator.SagaScheduleInput;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadExecutionShape;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ScenarioGenerator {

    private ScenarioGenerator() {
    }

    public static WorkloadGenerationResult generate(List<SagaDefinition> sagaDefinitions,
                                                    List<InputVariant> inputVariants,
                                                    ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();

        List<SagaDefinition> normalizedSagas = normalizeSagaDefinitions(sagaDefinitions, warnings);
        InputVariantNormalizer.NormalizationResult normalizedInputs = InputVariantNormalizer.normalize(inputVariants, effectiveConfig);
        mergeCounts(counts, normalizedInputs.counts());
        warnings.addAll(normalizedInputs.warnings());

        if (effectiveConfig.catalogWriteMode() == ScenarioGeneratorConfig.CatalogWriteMode.COUNT_ONLY) {
            putDefaultCounts(counts);
            counts.put("workloadsEmitted", 0);
            return new WorkloadGenerationResult(
                    WorkloadPlan.SCHEMA_VERSION,
                    effectiveConfig,
                    List.of(),
                    normalizedInputs.rejectedInputVariants(),
                    Collections.unmodifiableMap(counts),
                    List.copyOf(warnings));
        }

        ConflictGraphBuilder.Result conflictGraph = ConflictGraphBuilder.build(normalizedSagas, effectiveConfig);
        mergeCounts(counts, conflictGraph.counts());
        warnings.addAll(conflictGraph.warnings());

        Map<String, SagaDefinition> sagaByFqn = indexSagas(normalizedSagas);
        List<String> usableSagaFqns = normalizedInputs.inputsBySaga().keySet().stream()
                .filter(sagaByFqn::containsKey)
                .sorted()
                .toList();

        LinkedHashMap<String, WorkloadPlan> workloadsById = new LinkedHashMap<>();

        if (effectiveConfig.includeSingles()) {
            emitSingleSagaWorkloads(effectiveConfig, sagaByFqn, normalizedInputs.inputsBySaga(), workloadsById, warnings, counts);
        }

        if (workloadsById.size() < Math.max(0, effectiveConfig.maxCatalogScenarios())
                && effectiveConfig.maxSagaSetSize() >= 2
                && usableSagaFqns.size() >= 2) {
            emitMultiSagaWorkloads(effectiveConfig, sagaByFqn, normalizedInputs.inputsBySaga(), usableSagaFqns, conflictGraph, workloadsById, warnings, counts);
        }

        counts.put("workloadsEmitted", workloadsById.size());
        putDefaultCounts(counts);

        List<WorkloadPlan> workloadPlans = new ArrayList<>(workloadsById.values());
        workloadPlans.sort(Comparator
                .comparing(WorkloadPlan::kind, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(WorkloadPlan::deterministicId, Comparator.nullsFirst(String::compareTo)));

        LinkedHashSet<String> resultWarnings = new LinkedHashSet<>(warnings);
        for (WorkloadPlan workload : workloadPlans) {
            resultWarnings.addAll(workload.warnings());
        }

        return new WorkloadGenerationResult(
                WorkloadPlan.SCHEMA_VERSION,
                effectiveConfig,
                List.copyOf(workloadPlans),
                normalizedInputs.rejectedInputVariants(),
                Collections.unmodifiableMap(counts),
                List.copyOf(resultWarnings));
    }

    private static void emitSingleSagaWorkloads(ScenarioGeneratorConfig config,
                                                Map<String, SagaDefinition> sagaByFqn,
                                                Map<String, List<InputVariant>> inputsBySaga,
                                                LinkedHashMap<String, WorkloadPlan> workloadsById,
                                                LinkedHashSet<String> warnings,
                                                Map<String, Integer> counts) {
        int emitted = 0;
        int maxCatalogScenarios = Math.max(0, config.maxCatalogScenarios());
        for (String sagaFqn : inputsBySaga.keySet().stream().sorted().toList()) {
            if (workloadsById.size() >= maxCatalogScenarios) {
                counts.merge("singleWorkloadsEmitted", emitted, Integer::sum);
                capWorkloadPlans(warnings, counts, maxCatalogScenarios);
                return;
            }

            SagaDefinition saga = sagaByFqn.get(sagaFqn);
            if (saga == null) {
                continue;
            }

            List<InputVariant> sagaInputs = inputsBySaga.getOrDefault(sagaFqn, List.of());
            for (InputVariant input : sagaInputs) {
                if (workloadsById.size() >= maxCatalogScenarios) {
                    counts.merge("singleWorkloadsEmitted", emitted, Integer::sum);
                    capWorkloadPlans(warnings, counts, maxCatalogScenarios);
                    return;
                }

                WorkloadPlan workload = buildSingleSagaWorkload(config, saga, input);
                emitted += emitWorkload(workload, workloadsById, counts) ? 1 : 0;
            }
        }

        counts.merge("singleWorkloadsEmitted", emitted, Integer::sum);
    }

    private static void emitMultiSagaWorkloads(ScenarioGeneratorConfig config,
                                               Map<String, SagaDefinition> sagaByFqn,
                                               Map<String, List<InputVariant>> inputsBySaga,
                                               List<String> usableSagaFqns,
                                               ConflictGraphBuilder.Result conflictGraph,
                                               LinkedHashMap<String, WorkloadPlan> workloadsById,
                                               LinkedHashSet<String> warnings,
                                               Map<String, Integer> counts) {
        int maxCatalogScenarios = Math.max(0, config.maxCatalogScenarios());
        int emitted = 0;
        List<List<String>> sagaSets;
        if (config.generationStrategy() == ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE) {
            sagaSets = allInputBoundSagaSets(usableSagaFqns, config.maxSagaSetSize());
        } else {
            ConnectedSagaSetEnumerator.Result setResult = ConnectedSagaSetEnumerator.enumerate(usableSagaFqns, conflictGraph.adjacency(), config.maxSagaSetSize());
            mergeCounts(counts, setResult.counts());
            warnings.addAll(setResult.warnings());
            sagaSets = setResult.connectedSagaSets();
        }

        for (List<String> sagaSet : sagaSets) {
            if (workloadsById.size() >= maxCatalogScenarios) {
                counts.merge("multiWorkloadsEmitted", emitted, Integer::sum);
                capWorkloadPlans(warnings, counts, maxCatalogScenarios);
                return;
            }

            InputTupleJoiner.Result tupleResult = InputTupleJoiner.join(sagaSet, inputsBySaga);
            mergeCounts(counts, tupleResult.counts());
            warnings.addAll(tupleResult.warnings());

            for (InputTuple tuple : tupleResult.tuples()) {
                if (workloadsById.size() >= maxCatalogScenarios) {
                    counts.merge("multiWorkloadsEmitted", emitted, Integer::sum);
                    capWorkloadPlans(warnings, counts, maxCatalogScenarios);
                    return;
                }

                List<SagaScheduleInput> scheduleInputs = buildScheduleInputs(sagaSet, tuple.inputs(), sagaByFqn);
                List<ConflictCandidate> selectedCandidates = conflictGraph.conflictCandidates().stream()
                        .filter(candidate -> sagaSet.contains(candidate.leftSagaFqn()) && sagaSet.contains(candidate.rightSagaFqn()))
                        .toList();
                ScheduleEnumerator.Result scheduleResult = ScheduleEnumerator.enumerate(
                        scheduleInputs,
                        config.scheduleStrategy(),
                        config.maxSchedulesPerInputTuple(),
                        config.deterministicSeed(),
                        selectedCandidates);
                mergeCounts(counts, scheduleResult.counts());
                warnings.addAll(scheduleResult.warnings());

                for (List<ScheduledStep> schedule : scheduleResult.schedules()) {
                    if (workloadsById.size() >= maxCatalogScenarios) {
                        counts.merge("multiWorkloadsEmitted", emitted, Integer::sum);
                        capWorkloadPlans(warnings, counts, maxCatalogScenarios);
                        return;
                    }

                    WorkloadPlan workload = buildMultiSagaWorkload(
                            sagaSet,
                            tuple,
                            scheduleInputs,
                            schedule,
                            selectedCandidates,
                            config.generationStrategy() != ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE);
                    if (workload != null) {
                        if (emitWorkload(workload, workloadsById, counts)) {
                            emitted++;
                        }
                    }
                }
            }
        }

        counts.merge("multiWorkloadsEmitted", emitted, Integer::sum);
    }

    private static WorkloadPlan buildSingleSagaWorkload(ScenarioGeneratorConfig config, SagaDefinition saga, InputVariant input) {
        String sagaInstanceId = ScenarioIdGenerator.sagaInstanceId(saga.sagaFqn(), input.deterministicId());
        SagaInstance sagaInstance = new SagaInstance(sagaInstanceId, saga.sagaFqn(), input.deterministicId(), mergeWarnings(saga.warnings(), input.warnings()));
        SagaScheduleInput scheduleInput = new SagaScheduleInput(sagaInstance.deterministicId(), saga.sagaFqn(), sortedSteps(saga));
        ScheduleEnumerator.Result scheduleResult = ScheduleEnumerator.enumerate(
                List.of(scheduleInput),
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                1,
                config.deterministicSeed());
        List<ScheduledStep> forwardSchedule = scheduleResult.schedules().isEmpty() ? List.of() : scheduleResult.schedules().get(0);
        return buildWorkload(
                ScenarioKind.SINGLE_SAGA,
                List.of(sagaInstance),
                List.of(input),
                forwardSchedule,
                List.of(),
                List.of(scheduleInput),
                mergeWarnings(saga.warnings(), input.warnings(), scheduleResult.warnings()));
    }

    private static WorkloadPlan buildMultiSagaWorkload(List<String> connectedSet,
                                                    InputTuple tuple,
                                                    List<SagaScheduleInput> scheduleInputs,
                                                    List<ScheduledStep> schedule,
                                                    List<ConflictCandidate> selectedCandidates,
                                                    boolean requireConflictEvidence) {
        Map<String, String> sagaInstanceIdBySagaFqn = new LinkedHashMap<>();
        for (int index = 0; index < connectedSet.size(); index++) {
            sagaInstanceIdBySagaFqn.put(connectedSet.get(index), ScenarioIdGenerator.sagaInstanceId(connectedSet.get(index), tuple.inputs().get(index).deterministicId()));
        }

        Map<String, ScheduledStep> scheduledByStepId = new LinkedHashMap<>();
        for (ScheduledStep scheduledStep : schedule) {
            scheduledByStepId.put(scheduledStep.sagaInstanceId() + "|" + scheduledStep.stepId(), scheduledStep);
        }

        List<ConflictEvidence> conflictEvidence = new ArrayList<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        for (ConflictCandidate candidate : selectedCandidates) {
            String leftSagaInstanceId = sagaInstanceIdBySagaFqn.get(candidate.leftSagaFqn());
            String rightSagaInstanceId = sagaInstanceIdBySagaFqn.get(candidate.rightSagaFqn());
            ScheduledStep leftScheduledStep = scheduledByStepId.get(leftSagaInstanceId + "|" + candidate.leftStepId());
            ScheduledStep rightScheduledStep = scheduledByStepId.get(rightSagaInstanceId + "|" + candidate.rightStepId());
            if (leftScheduledStep == null || rightScheduledStep == null) {
                continue;
            }

            ConflictEvidence evidence = new ConflictEvidence(
                    candidate.deterministicId(),
                    leftScheduledStep.deterministicId(),
                    rightScheduledStep.deterministicId(),
                    candidate.leftFootprint().aggregateKey(),
                    candidate.rightFootprint().aggregateKey(),
                    candidate.leftFootprint().accessMode(),
                    candidate.rightFootprint().accessMode(),
                    candidate.kind(),
                    candidate.warnings());
            conflictEvidence.add(evidence);
            warnings.addAll(candidate.warnings());
        }

        if (requireConflictEvidence && conflictEvidence.isEmpty()) {
            return null;
        }

        List<SagaInstance> sagaInstances = new ArrayList<>();
        for (int index = 0; index < connectedSet.size(); index++) {
            String sagaFqn = connectedSet.get(index);
            InputVariant input = tuple.inputs().get(index);
            sagaInstances.add(new SagaInstance(
                    ScenarioIdGenerator.sagaInstanceId(sagaFqn, input.deterministicId()),
                    sagaFqn,
                    input.deterministicId(),
                    mergeWarnings(input.warnings())));
        }

        List<String> workloadWarnings = new ArrayList<>(mergeWarnings(
                tuple.warnings(),
                schedule.stream().flatMap(step -> step.warnings().stream()).toList(),
                conflictEvidence.stream().flatMap(evidence -> evidence.warnings().stream()).toList()));
        workloadWarnings.addAll(warnings);

        return buildWorkload(
                ScenarioKind.MULTI_SAGA,
                sagaInstances,
                tuple.inputs(),
                schedule,
                conflictEvidence,
                scheduleInputs,
                workloadWarnings);
    }

    private static WorkloadPlan buildWorkload(ScenarioKind kind,
                                              List<SagaInstance> participants,
                                              List<InputVariant> acceptedInputs,
                                              List<ScheduledStep> forwardSchedule,
                                              List<ConflictEvidence> conflictEvidence,
                                              List<SagaScheduleInput> scheduleInputs,
                                              List<String> warnings) {
        Map<String, StepDefinition> definitionsByOccurrenceAnchor = new LinkedHashMap<>();
        for (SagaScheduleInput input : scheduleInputs) {
            for (StepDefinition definition : input.steps()) {
                definitionsByOccurrenceAnchor.put(input.sagaInstanceId() + "|" + definition.deterministicId(), definition);
            }
        }

        List<ForwardFaultSlot> faultSlots = new ArrayList<>();
        List<CompensationCheckpoint> checkpoints = new ArrayList<>();
        for (int index = 0; index < forwardSchedule.size(); index++) {
            ScheduledStep scheduledStep = forwardSchedule.get(index);
            faultSlots.add(new ForwardFaultSlot(
                    ScenarioIdGenerator.forwardFaultSlotId(index, scheduledStep),
                    index,
                    scheduledStep.deterministicId(),
                    scheduledStep.sagaInstanceId(),
                    scheduledStep.stepId(),
                    scheduledStep.runtimeStepName(),
                    scheduledStep.deterministicId()));

            StepDefinition definition = definitionsByOccurrenceAnchor.get(
                    scheduledStep.sagaInstanceId() + "|" + scheduledStep.stepId());
            if (definition != null && definition.compensationEvidence() != null) {
                int checkpointIndex = checkpoints.size();
                checkpoints.add(new CompensationCheckpoint(
                        ScenarioIdGenerator.compensationCheckpointId(checkpointIndex, scheduledStep, definition),
                        checkpointIndex,
                        scheduledStep.sagaInstanceId(),
                        scheduledStep.deterministicId(),
                        scheduledStep.stepId(),
                        scheduledStep.runtimeStepName(),
                        scheduledStep.deterministicId(),
                        definition.compensationEvidence(),
                        definition.footprints(),
                        definition.compensationFootprints(),
                        mergeWarnings(definition.analysisDiagnostics(), definition.warnings())));
            }
        }

        WorkloadPlan withoutId = new WorkloadPlan(
                WorkloadPlan.SCHEMA_VERSION,
                null,
                kind,
                WorkloadExecutionShape.SAGA_LOCAL,
                participants,
                acceptedInputs,
                forwardSchedule,
                conflictEvidence,
                faultSlots,
                checkpoints,
                warnings);
        return new WorkloadPlan(
                withoutId.schemaVersion(),
                ScenarioIdGenerator.workloadPlanId(withoutId),
                withoutId.kind(),
                withoutId.executionShape(),
                withoutId.participants(),
                withoutId.acceptedInputs(),
                withoutId.forwardSchedule(),
                withoutId.conflictEvidence(),
                withoutId.faultSlots(),
                withoutId.compensationCheckpoints(),
                withoutId.warnings());
    }

    private static List<SagaScheduleInput> buildScheduleInputs(List<String> connectedSet,
                                                               List<InputVariant> inputs,
                                                               Map<String, SagaDefinition> sagaByFqn) {
        List<SagaScheduleInput> scheduleInputs = new ArrayList<>();
        for (int index = 0; index < connectedSet.size(); index++) {
            String sagaFqn = connectedSet.get(index);
            SagaDefinition saga = sagaByFqn.get(sagaFqn);
            if (saga == null) {
                continue;
            }
            InputVariant input = inputs.get(index);
            scheduleInputs.add(new SagaScheduleInput(
                    ScenarioIdGenerator.sagaInstanceId(sagaFqn, input.deterministicId()),
                    sagaFqn,
                    sortedSteps(saga)));
        }
        return List.copyOf(scheduleInputs);
    }

    private static List<StepDefinition> sortedSteps(SagaDefinition saga) {
        return saga.steps().stream()
                .sorted(Comparator
                        .comparingInt(StepDefinition::orderIndex)
                        .thenComparing(StepDefinition::deterministicId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(StepDefinition::stepKey, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(StepDefinition::name, Comparator.nullsFirst(String::compareTo)))
                .map(step -> normalizeStep(saga.sagaFqn(), step))
                .toList();
    }

    private static StepDefinition normalizeStep(String sagaFqn, StepDefinition step) {
        String deterministicId = ScenarioIdGenerator.stepDefinitionId(sagaFqn, step);
        return new StepDefinition(
                deterministicId,
                normalize(step.stepKey()),
                normalize(step.name()),
                step.orderIndex(),
                step.predecessorStepKeys(),
                normalizeFootprints(step.footprints()),
                normalizeFootprints(step.compensationFootprints()),
                step.compensationRegistered(),
                step.forwardAnalysisComplete(),
                step.compensationAnalysisComplete(),
                step.compensationEvidence(),
                mergeWarnings(step.analysisDiagnostics()),
                mergeWarnings(step.warnings()));
    }

    private static List<StepFootprint> normalizeFootprints(List<StepFootprint> footprints) {
        if (footprints == null || footprints.isEmpty()) {
            return List.of();
        }
        return footprints.stream().map(footprint -> new StepFootprint(
                footprint.aggregateKey(),
                footprint.accessMode(),
                mergeWarnings(footprint.warnings()))).toList();
    }

    private static Map<String, SagaDefinition> indexSagas(List<SagaDefinition> sagas) {
        LinkedHashMap<String, SagaDefinition> indexed = new LinkedHashMap<>();
        for (SagaDefinition saga : sagas) {
            if (saga != null && saga.sagaFqn() != null) {
                indexed.putIfAbsent(saga.sagaFqn(), saga);
            }
        }
        return indexed;
    }

    private static List<SagaDefinition> normalizeSagaDefinitions(List<SagaDefinition> sagaDefinitions, LinkedHashSet<String> warnings) {
        List<SagaDefinition> safeSagas = sagaDefinitions == null ? List.of() : sagaDefinitions;
        LinkedHashMap<String, SagaDefinition> deduped = new LinkedHashMap<>();
        for (SagaDefinition rawSaga : safeSagas.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn, Comparator.nullsFirst(String::compareTo)))
                .toList()) {
            if (rawSaga.sagaFqn() == null) {
                warnings.add("ignored saga definition without saga FQN");
                continue;
            }

            SagaDefinition normalized = new SagaDefinition(
                    rawSaga.sagaFqn(),
                    sortedSteps(rawSaga),
                    mergeWarnings(rawSaga.warnings()));
            SagaDefinition existing = deduped.putIfAbsent(normalized.sagaFqn(), normalized);
            if (existing != null) {
                warnings.add("deduplicated saga definition " + normalized.sagaFqn());
            }
        }
        return List.copyOf(deduped.values());
    }

    private static boolean emitWorkload(WorkloadPlan workload, LinkedHashMap<String, WorkloadPlan> workloadsById, Map<String, Integer> counts) {
        WorkloadPlan existing = workloadsById.putIfAbsent(workload.deterministicId(), workload);
        if (existing == null) {
            counts.merge("workloadPlansEmitted", 1, Integer::sum);
            return true;
        }
        counts.merge("workloadPlansDeduplicated", 1, Integer::sum);
        return false;
    }

    private static void capWorkloadPlans(LinkedHashSet<String> warnings, Map<String, Integer> counts, int maxCatalogScenarios) {
        warnings.add("reached maxCatalogScenarios=" + maxCatalogScenarios + "; remaining workloads were not emitted");
        counts.putIfAbsent("workloadsCapped", 0);
        counts.put("workloadsCapped", counts.get("workloadsCapped") + 1);
    }

    private static List<List<String>> allInputBoundSagaSets(List<String> usableSagaFqns, int maxSagaSetSize) {
        List<List<String>> sagaSets = new ArrayList<>();
        int maxSize = Math.max(0, Math.min(maxSagaSetSize, usableSagaFqns.size()));
        for (int size = 2; size <= maxSize; size++) {
            collectCombinations(usableSagaFqns, size, 0, new ArrayList<>(), sagaSets);
        }
        return sagaSets;
    }

    private static void collectCombinations(List<String> values,
                                            int targetSize,
                                            int startIndex,
                                            List<String> current,
                                            List<List<String>> combinations) {
        if (current.size() == targetSize) {
            combinations.add(List.copyOf(current));
            return;
        }
        for (int index = startIndex; index < values.size(); index++) {
            current.add(values.get(index));
            collectCombinations(values, targetSize, index + 1, current, combinations);
            current.remove(current.size() - 1);
        }
    }

    private static void putDefaultCounts(Map<String, Integer> counts) {
        counts.putIfAbsent("workloadsCapped", 0);
        counts.putIfAbsent("workloadPlansEmitted", 0);
        counts.putIfAbsent("workloadPlansDeduplicated", 0);
        counts.putIfAbsent("singleWorkloadsEmitted", 0);
        counts.putIfAbsent("multiWorkloadsEmitted", 0);
        counts.putIfAbsent("connectedSagaSetsSeen", 0);
        counts.putIfAbsent("connectedSagaSetsEmitted", 0);
        counts.putIfAbsent("connectedSagaSetsPruned", 0);
        counts.putIfAbsent("inputTuplesSeen", 0);
        counts.putIfAbsent("inputTuplesEmitted", 0);
        counts.putIfAbsent("inputTuplesDeduplicated", 0);
        counts.putIfAbsent("schedulesSeen", 0);
        counts.putIfAbsent("schedulesEmitted", 0);
        counts.putIfAbsent("schedulesCapped", 0);
    }

    private static void mergeCounts(Map<String, Integer> target, Map<String, Integer> source) {
        if (source == null || source.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            target.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    private static List<String> mergeWarnings(List<String>... warningLists) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (warningLists != null) {
            for (List<String> warningList : warningLists) {
                if (warningList != null) {
                    merged.addAll(warningList);
                }
            }
        }
        return List.copyOf(merged);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
