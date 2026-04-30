package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConflictGraphBuilder.ConflictCandidate;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner.InputTuple;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScheduleEnumerator.SagaScheduleInput;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;
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

    public static ScenarioGenerationResult generate(List<SagaDefinition> sagaDefinitions,
                                                    List<InputVariant> inputVariants,
                                                    ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();

        List<SagaDefinition> normalizedSagas = normalizeSagaDefinitions(sagaDefinitions, warnings);
        InputVariantNormalizer.NormalizationResult normalizedInputs = InputVariantNormalizer.normalize(inputVariants, effectiveConfig);
        mergeCounts(counts, normalizedInputs.counts());
        warnings.addAll(normalizedInputs.warnings());

        ConflictGraphBuilder.Result conflictGraph = ConflictGraphBuilder.build(normalizedSagas, effectiveConfig);
        mergeCounts(counts, conflictGraph.counts());
        warnings.addAll(conflictGraph.warnings());

        Map<String, SagaDefinition> sagaByFqn = indexSagas(normalizedSagas);
        List<String> usableSagaFqns = normalizedInputs.inputsBySaga().keySet().stream()
                .filter(sagaByFqn::containsKey)
                .sorted()
                .toList();

        LinkedHashMap<String, ScenarioPlan> plansById = new LinkedHashMap<>();

        if (effectiveConfig.includeSingles()) {
            emitSingleSagaScenarios(effectiveConfig, sagaByFqn, normalizedInputs.inputsBySaga(), plansById, warnings, counts);
        }

        if (plansById.size() < Math.max(0, effectiveConfig.maxScenarios())
                && effectiveConfig.maxSagaSetSize() >= 2
                && usableSagaFqns.size() >= 2) {
            emitMultiSagaScenarios(effectiveConfig, sagaByFqn, normalizedInputs.inputsBySaga(), usableSagaFqns, conflictGraph, plansById, warnings, counts);
        }

        counts.put("scenariosEmitted", plansById.size());
        counts.putIfAbsent("scenariosCapped", 0);
        counts.putIfAbsent("scenarioPlansEmitted", 0);
        counts.putIfAbsent("scenarioPlansDeduplicated", 0);
        counts.putIfAbsent("singleScenariosEmitted", 0);
        counts.putIfAbsent("multiScenariosEmitted", 0);
        counts.putIfAbsent("connectedSagaSetsSeen", 0);
        counts.putIfAbsent("connectedSagaSetsEmitted", 0);
        counts.putIfAbsent("connectedSagaSetsPruned", 0);
        counts.putIfAbsent("inputTuplesSeen", 0);
        counts.putIfAbsent("inputTuplesEmitted", 0);
        counts.putIfAbsent("inputTuplesDeduplicated", 0);
        counts.putIfAbsent("schedulesSeen", 0);
        counts.putIfAbsent("schedulesEmitted", 0);
        counts.putIfAbsent("schedulesCapped", 0);

        List<ScenarioPlan> scenarioPlans = new ArrayList<>(plansById.values());
        scenarioPlans.sort(Comparator
                .comparing(ScenarioPlan::kind, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(ScenarioPlan::deterministicId, Comparator.nullsFirst(String::compareTo)));

        LinkedHashSet<String> resultWarnings = new LinkedHashSet<>(warnings);
        for (ScenarioPlan plan : scenarioPlans) {
            resultWarnings.addAll(plan.warnings());
        }

        return new ScenarioGenerationResult(
                ScenarioPlan.SCHEMA_VERSION,
                effectiveConfig,
                List.copyOf(scenarioPlans),
                normalizedInputs.rejectedInputVariants(),
                Collections.unmodifiableMap(counts),
                List.copyOf(resultWarnings));
    }

    private static void emitSingleSagaScenarios(ScenarioGeneratorConfig config,
                                                Map<String, SagaDefinition> sagaByFqn,
                                                Map<String, List<InputVariant>> inputsBySaga,
                                                LinkedHashMap<String, ScenarioPlan> plansById,
                                                LinkedHashSet<String> warnings,
                                                Map<String, Integer> counts) {
        int emitted = 0;
        int maxScenarios = Math.max(0, config.maxScenarios());
        for (String sagaFqn : inputsBySaga.keySet().stream().sorted().toList()) {
            if (plansById.size() >= maxScenarios) {
                counts.merge("singleScenariosEmitted", emitted, Integer::sum);
                capScenarioPlans(warnings, counts, maxScenarios);
                return;
            }

            SagaDefinition saga = sagaByFqn.get(sagaFqn);
            if (saga == null) {
                continue;
            }

            List<InputVariant> sagaInputs = inputsBySaga.getOrDefault(sagaFqn, List.of());
            for (InputVariant input : sagaInputs) {
                if (plansById.size() >= maxScenarios) {
                    counts.merge("singleScenariosEmitted", emitted, Integer::sum);
                    capScenarioPlans(warnings, counts, maxScenarios);
                    return;
                }

                ScenarioPlan plan = buildSingleSagaPlan(config, saga, input);
                emitted += emitPlan(plan, plansById, counts) ? 1 : 0;
            }
        }

        counts.merge("singleScenariosEmitted", emitted, Integer::sum);
    }

    private static void emitMultiSagaScenarios(ScenarioGeneratorConfig config,
                                               Map<String, SagaDefinition> sagaByFqn,
                                               Map<String, List<InputVariant>> inputsBySaga,
                                               List<String> usableSagaFqns,
                                               ConflictGraphBuilder.Result conflictGraph,
                                               LinkedHashMap<String, ScenarioPlan> plansById,
                                               LinkedHashSet<String> warnings,
                                               Map<String, Integer> counts) {
        int maxScenarios = Math.max(0, config.maxScenarios());
        int emitted = 0;
        ConnectedSagaSetEnumerator.Result setResult = ConnectedSagaSetEnumerator.enumerate(usableSagaFqns, conflictGraph.adjacency(), config.maxSagaSetSize());
        mergeCounts(counts, setResult.counts());
        warnings.addAll(setResult.warnings());

        for (List<String> connectedSet : setResult.connectedSagaSets()) {
            if (plansById.size() >= maxScenarios) {
                counts.merge("multiScenariosEmitted", emitted, Integer::sum);
                capScenarioPlans(warnings, counts, maxScenarios);
                return;
            }

            InputTupleJoiner.Result tupleResult = InputTupleJoiner.join(connectedSet, inputsBySaga);
            mergeCounts(counts, tupleResult.counts());
            warnings.addAll(tupleResult.warnings());

            for (InputTuple tuple : tupleResult.tuples()) {
                if (plansById.size() >= maxScenarios) {
                    counts.merge("multiScenariosEmitted", emitted, Integer::sum);
                    capScenarioPlans(warnings, counts, maxScenarios);
                    return;
                }

                List<SagaScheduleInput> scheduleInputs = buildScheduleInputs(connectedSet, tuple.inputs(), sagaByFqn);
                ScheduleEnumerator.Result scheduleResult = ScheduleEnumerator.enumerate(
                        scheduleInputs,
                        config.scheduleStrategy(),
                        config.maxSchedulesPerInputTuple(),
                        config.deterministicSeed());
                mergeCounts(counts, scheduleResult.counts());
                warnings.addAll(scheduleResult.warnings());

                List<ConflictCandidate> selectedCandidates = conflictGraph.conflictCandidates().stream()
                        .filter(candidate -> connectedSet.contains(candidate.leftSagaFqn()) && connectedSet.contains(candidate.rightSagaFqn()))
                        .toList();

                for (List<ScheduledStep> schedule : scheduleResult.schedules()) {
                    if (plansById.size() >= maxScenarios) {
                        counts.merge("multiScenariosEmitted", emitted, Integer::sum);
                        capScenarioPlans(warnings, counts, maxScenarios);
                        return;
                    }

                    ScenarioPlan plan = buildMultiSagaPlan(connectedSet, tuple, scheduleInputs, schedule, selectedCandidates);
                    if (plan != null) {
                        if (emitPlan(plan, plansById, counts)) {
                            emitted++;
                        }
                    }
                }
            }
        }

        counts.merge("multiScenariosEmitted", emitted, Integer::sum);
    }

    private static ScenarioPlan buildSingleSagaPlan(ScenarioGeneratorConfig config, SagaDefinition saga, InputVariant input) {
        String sagaInstanceId = ScenarioIdGenerator.sagaInstanceId(saga.sagaFqn(), input.deterministicId());
        SagaInstance sagaInstance = new SagaInstance(sagaInstanceId, saga.sagaFqn(), input.deterministicId(), mergeWarnings(saga.warnings(), input.warnings()));
        ScheduleEnumerator.Result scheduleResult = ScheduleEnumerator.enumerate(
                List.of(new SagaScheduleInput(sagaInstance.deterministicId(), saga.sagaFqn(), sortedSteps(saga))),
                ScenarioGeneratorConfig.ScheduleStrategy.SERIAL,
                1,
                config.deterministicSeed());
        List<ScheduledStep> expandedSchedule = scheduleResult.schedules().isEmpty() ? List.of() : scheduleResult.schedules().get(0);
        List<ConflictEvidence> conflictEvidence = List.of();
        List<String> planWarnings = mergeWarnings(saga.warnings(), input.warnings(), scheduleResult.warnings());
        String deterministicId = ScenarioIdGenerator.scenarioPlanId(
                ScenarioKind.SINGLE_SAGA,
                List.of(sagaInstance),
                List.of(input),
                expandedSchedule,
                conflictEvidence);

        return new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                deterministicId,
                ScenarioKind.SINGLE_SAGA,
                List.of(sagaInstance),
                List.of(input),
                expandedSchedule,
                null,
                conflictEvidence,
                planWarnings);
    }

    private static ScenarioPlan buildMultiSagaPlan(List<String> connectedSet,
                                                   InputTuple tuple,
                                                   List<SagaScheduleInput> scheduleInputs,
                                                   List<ScheduledStep> schedule,
                                                   List<ConflictCandidate> selectedCandidates) {
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

        if (conflictEvidence.isEmpty()) {
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

        List<String> planWarnings = new ArrayList<>(mergeWarnings(
                tuple.warnings(),
                schedule.stream().flatMap(step -> step.warnings().stream()).toList(),
                conflictEvidence.stream().flatMap(evidence -> evidence.warnings().stream()).toList()));
        planWarnings.addAll(warnings);

        String deterministicId = ScenarioIdGenerator.scenarioPlanId(
                ScenarioKind.MULTI_SAGA,
                sagaInstances,
                tuple.inputs(),
                schedule,
                conflictEvidence);

        return new ScenarioPlan(
                ScenarioPlan.SCHEMA_VERSION,
                deterministicId,
                ScenarioKind.MULTI_SAGA,
                sagaInstances,
                tuple.inputs(),
                schedule,
                null,
                conflictEvidence,
                planWarnings);
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

    private static boolean emitPlan(ScenarioPlan plan, LinkedHashMap<String, ScenarioPlan> plansById, Map<String, Integer> counts) {
        ScenarioPlan existing = plansById.putIfAbsent(plan.deterministicId(), plan);
        if (existing == null) {
            counts.merge("scenarioPlansEmitted", 1, Integer::sum);
            return true;
        }
        counts.merge("scenarioPlansDeduplicated", 1, Integer::sum);
        return false;
    }

    private static void capScenarioPlans(LinkedHashSet<String> warnings, Map<String, Integer> counts, int maxScenarios) {
        warnings.add("reached maxScenarios=" + maxScenarios + "; remaining scenarios were not emitted");
        counts.putIfAbsent("scenariosCapped", 0);
        counts.put("scenariosCapped", counts.get("scenariosCapped") + 1);
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
