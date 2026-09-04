package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConflictGraphBuilder;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ConnectedSagaSetEnumerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputVariantNormalizer;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleSelection;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.InputTupleJoiner;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.SetupPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioIdGenerator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.GroupedSagaSetRow;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.ExecutorReadiness;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.InteractionCoverage;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.InputBoundScenarioSpace;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.InteractionSummary;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.ScenarioSpaceTotals;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.TopContributor;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport.TypeLevelCoverage;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SourceSetupPlanBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadExecutionShape;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceAggregateKeyInputEvidence;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class ScenarioSpaceAccountingCalculator {

    private static final int TOP_CONTRIBUTOR_LIMIT = 10;
    public ScenarioSpaceAccountingReport calculate(String targetApplication,
                                                   List<SagaDefinition> sagaDefinitions,
                                                   List<InputVariant> inputVariants,
                                                   ScenarioGeneratorConfig config,
                                                   int catalogWritten) {
        return calculate(targetApplication, sagaDefinitions, inputVariants, List.of(), List.of(), config, catalogWritten);
    }

    public ScenarioSpaceAccountingReport calculate(String targetApplication,
                                                   List<SagaDefinition> sagaDefinitions,
                                                   List<InputVariant> inputVariants,
                                                   List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                                   ScenarioGeneratorConfig config,
                                                   int catalogWritten) {
        return calculate(targetApplication, sagaDefinitions, inputVariants, List.of(), aggregateKeyInputEvidence,
                config, catalogWritten);
    }

    public ScenarioSpaceAccountingReport calculate(String targetApplication,
                                                   List<SagaDefinition> sagaDefinitions,
                                                   List<InputVariant> inputVariants,
                                                   List<SourceSetupPlanBinding> sourceSetupPlanBindings,
                                                   List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                                   ScenarioGeneratorConfig config,
                                                   int catalogWritten) {
        ScenarioGeneratorConfig effectiveConfig = config == null ? new ScenarioGeneratorConfig() : config;
        Map<String, SagaDefinition> sagaByFqn = indexSagas(sagaDefinitions);
        InputVariantNormalizer.NormalizationResult normalizedInputs = InputVariantNormalizer.normalize(inputVariants, effectiveConfig);
        Map<String, List<InputVariant>> inputsBySaga = acceptedInputsByKnownSaga(normalizedInputs.inputsBySaga(), sagaByFqn);
        GraphViews graphViews = buildGraphViews(sagaByFqn.values().stream().toList(), effectiveConfig);

        List<CalculatedRow> calculatedRows = buildGroupedRows(sagaByFqn, inputsBySaga,
                sourceSetupPlanBindings, aggregateKeyInputEvidence, effectiveConfig, graphViews);
        List<GroupedSagaSetRow> groupedRows = calculatedRows.stream().map(CalculatedRow::row).toList();
        ScenarioSpaceTotals allInputBound = totals(calculatedRows, false);
        ScenarioSpaceTotals selectedByGenerator = totals(calculatedRows, true);
        ScenarioSpaceTotals written = new ScenarioSpaceTotals(Integer.toString(Math.max(0, catalogWritten)), Map.of());
        InputTupleSelection.Mode selectedMode = selectionMode(effectiveConfig);
        ScenarioSpaceAccountingReport.SetupCoverage setupCoverage = selectedMode == InputTupleSelection.Mode.ALL
                ? null : setupCoverage(calculatedRows);

        return new ScenarioSpaceAccountingReport(
                ScenarioSpaceAccountingReport.SCHEMA_VERSION,
                ScenarioSpaceAccountingReport.AccountingRunConfig.from(targetApplication, effectiveConfig),
                typeLevelCoverage(sagaByFqn, inputsBySaga, graphViews, effectiveConfig),
                executorReadiness(normalizedInputs.inputsBySaga()),
                new InputBoundScenarioSpace(allInputBound, selectedByGenerator, written, setupCoverage),
                groupedRows,
                topContributors(groupedRows));
    }

    private List<CalculatedRow> buildGroupedRows(Map<String, SagaDefinition> sagaByFqn,
                                                     Map<String, List<InputVariant>> inputsBySaga,
                                                     List<SourceSetupPlanBinding> sourceSetupPlanBindings,
                                                     List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                                     ScenarioGeneratorConfig config,
                                                     GraphViews graphViews) {
        List<String> sagaFqns = inputsBySaga.keySet().stream()
                .filter(sagaByFqn::containsKey)
                .sorted()
                .toList();
        int minSize = config.includeSingles() ? 1 : 2;
        int maxSize = Math.max(0, Math.min(config.maxSagaSetSize(), sagaFqns.size()));
        List<List<String>> sagaSets = new ArrayList<>();
        int threshold = Math.max(0, config.maxGroupedSagaSetRows());
        for (int size = minSize; size <= maxSize; size++) {
            collectCombinations(sagaFqns, size, 0, new ArrayList<>(), sagaSets, threshold);
        }

        List<CalculatedRow> rows = new ArrayList<>();
        for (List<String> sagaSet : sagaSets) {
            rows.add(buildRow(sagaSet, sagaByFqn, inputsBySaga, sourceSetupPlanBindings,
                    aggregateKeyInputEvidence, config, graphViews));
        }
        rows.sort(Comparator
                .comparingInt((CalculatedRow value) -> value.row().sagaSetSize())
                .thenComparing(value -> value.row().sagaSetKey()));
        return List.copyOf(rows);
    }

    private CalculatedRow buildRow(List<String> sagaSet,
                                       Map<String, SagaDefinition> sagaByFqn,
                                       Map<String, List<InputVariant>> inputsBySaga,
                                       List<SourceSetupPlanBinding> sourceSetupPlanBindings,
                                       List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                       ScenarioGeneratorConfig config,
                                       GraphViews graphViews) {
        LinkedHashMap<String, Integer> inputCountsBySaga = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> stepCountsBySaga = new LinkedHashMap<>();
        for (String sagaFqn : sagaSet) {
            inputCountsBySaga.put(sagaFqn, inputsBySaga.getOrDefault(sagaFqn, List.of()).size());
            stepCountsBySaga.put(sagaFqn, sagaByFqn.get(sagaFqn).steps().size());
        }

        BigInteger allTupleCount = InputTupleSelection.count(sagaSet, inputsBySaga, List.of(),
                aggregateKeyInputEvidence, InputTupleSelection.Mode.ALL);
        InputTupleSelection.Mode selectedMode = selectionMode(config);
        ConflictGraphBuilder.Result selectedGraph = selectedMode == InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK
                ? graphViews.broad() : graphViews.strict();
        BigInteger selectedTupleCount = InputTupleSelection.count(sagaSet, inputsBySaga,
                selectedGraph.conflictCandidates(), aggregateKeyInputEvidence, selectedMode);
        BigInteger scheduleCount = scheduleCountPerTuple(sagaSet, sagaByFqn, config, graphViews);
        BigInteger allShapeCount = allTupleCount.multiply(scheduleCount);
        BigInteger selectedShapeCount = selectedTupleCount.multiply(scheduleCount);
        InteractionSummary strictSummary = interactionSummary(sagaSet, graphViews.strict());
        InteractionSummary broadSummary = interactionSummary(sagaSet, graphViews.broad());
        boolean selected = selectedTupleCount.signum() > 0;
        SetupShapeCounts setupShapeCounts = selectedMode == InputTupleSelection.Mode.ALL
                ? null
                : setupShapeCounts(sagaSet, inputsBySaga, sourceSetupPlanBindings,
                aggregateKeyInputEvidence, selectedMode, selectedGraph, scheduleCount);

        GroupedSagaSetRow row = new GroupedSagaSetRow(
                sagaSetKey(sagaSet),
                sagaSet.size(),
                sagaSet,
                inputCountsBySaga,
                stepCountsBySaga,
                selectedTupleCount.toString(),
                scheduleCount.toString(),
                selectedShapeCount.toString(),
                strictSummary,
                broadSummary,
                selected);
        return new CalculatedRow(row, allShapeCount, selectedShapeCount, setupShapeCounts);
    }

    private InputTupleSelection.Mode selectionMode(ScenarioGeneratorConfig config) {
        if (config.generationStrategy() == ScenarioGeneratorConfig.GenerationStrategy.BRUTE_FORCE) {
            return InputTupleSelection.Mode.ALL;
        }
        return config.allowTypeOnlyFallback()
                ? InputTupleSelection.Mode.WITH_TYPE_ONLY_FALLBACK
                : InputTupleSelection.Mode.STRICT;
    }

    private SetupShapeCounts setupShapeCounts(List<String> sagaSet,
                                              Map<String, List<InputVariant>> inputsBySaga,
                                              List<SourceSetupPlanBinding> sourceSetupPlanBindings,
                                              List<SourceAggregateKeyInputEvidence> aggregateKeyInputEvidence,
                                              InputTupleSelection.Mode selectedMode,
                                              ConflictGraphBuilder.Result selectedGraph,
                                              BigInteger scheduleCount) {
        if (scheduleCount.signum() == 0) {
            return SetupShapeCounts.empty();
        }
        BigInteger[] totals = new BigInteger[]{BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO};
        InputTupleJoiner.forEachSelected(
                sagaSet, inputsBySaga, selectedGraph.conflictCandidates(), aggregateKeyInputEvidence, selectedMode, tuple -> {
            SetupPlan setup = ScenarioGenerator.setupPlanFor(tuple, sourceSetupPlanBindings);
            if (!tupleMaterializable(tuple, setup)) {
                totals[2] = totals[2].add(scheduleCount);
            } else if (setup != null) {
                totals[0] = totals[0].add(scheduleCount);
            } else {
                totals[1] = totals[1].add(scheduleCount);
            }
        });
        return new SetupShapeCounts(totals[0], totals[1], totals[2]);
    }

    /** Uses the same setup validation and participant-argument readiness path as eager generation. */
    private boolean tupleMaterializable(InputTupleJoiner.InputTuple tuple, SetupPlan setup) {
        if (setup != null && !new SetupPlanValidator().validate(setup, tuple.inputs()).valid()) {
            return false;
        }
        WorkloadPlan readinessPlan = new WorkloadPlan(
                WorkloadPlan.CURRENT_SCHEMA_VERSION,
                "accounting-readiness",
                tuple.inputs().size() == 1 ? ScenarioKind.SINGLE_SAGA : ScenarioKind.MULTI_SAGA,
                WorkloadExecutionShape.SAGA_LOCAL,
                List.of(), tuple.inputs(), List.of(), List.of(), List.of(), null, setup,
                List.of(), List.of(), List.of(), List.of());
        ScenarioExecutorReadinessEvaluator evaluator = new ScenarioExecutorReadinessEvaluator();
        return tuple.inputs().stream().allMatch(input -> evaluator.evaluate(readinessPlan, input).materializable());
    }

    private ScenarioSpaceAccountingReport.SetupCoverage setupCoverage(List<CalculatedRow> rows) {
        return new ScenarioSpaceAccountingReport.SetupCoverage(
                setupTotals(rows, SetupShapeCounts::withSourceSetup),
                setupTotals(rows, SetupShapeCounts::withoutSetup),
                setupTotals(rows, SetupShapeCounts::blocked));
    }

    private ScenarioSpaceTotals setupTotals(List<CalculatedRow> rows,
                                             java.util.function.Function<SetupShapeCounts, BigInteger> extractor) {
        LinkedHashMap<String, BigInteger> bySize = new LinkedHashMap<>();
        BigInteger total = BigInteger.ZERO;
        for (CalculatedRow row : rows) {
            if (row.setupShapeCounts() == null) {
                throw new IllegalStateException("setup totals require exact selected tuple classification");
            }
            BigInteger count = extractor.apply(row.setupShapeCounts());
            total = total.add(count);
            bySize.merge(Integer.toString(row.row().sagaSetSize()), count, BigInteger::add);
        }
        LinkedHashMap<String, String> serialized = new LinkedHashMap<>();
        bySize.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> serialized.put(entry.getKey(), entry.getValue().toString()));
        return new ScenarioSpaceTotals(total.toString(), serialized);
    }

    private InteractionSummary interactionSummary(List<String> sagaSet, ConflictGraphBuilder.Result graph) {
        java.util.Set<List<String>> directPairs = new java.util.LinkedHashSet<>();
        LinkedHashMap<String, Integer> evidenceKindCounts = new LinkedHashMap<>();
        for (ConflictGraphBuilder.ConflictCandidate candidate : graph.conflictCandidates()) {
            if (!sagaSet.contains(candidate.leftSagaFqn()) || !sagaSet.contains(candidate.rightSagaFqn())) {
                continue;
            }
            directPairs.add(sortedPair(candidate.leftSagaFqn(), candidate.rightSagaFqn()));
            String kind = candidate.kind() == null ? "UNKNOWN" : candidate.kind().name();
            evidenceKindCounts.merge(kind, 1, Integer::sum);
        }
        return new InteractionSummary(connectedInGraph(sagaSet, graph.adjacency()), directPairs.size(), evidenceKindCounts);
    }

    private boolean connectedInGraph(List<String> sagaSet, Map<String, Set<String>> adjacency) {
        if (sagaSet.size() <= 1) {
            return true;
        }
        return ConnectedSagaSetEnumerator.enumerate(sagaSet, adjacency, sagaSet.size()).connectedSagaSets().stream()
                .anyMatch(connectedSet -> connectedSet.size() == sagaSet.size() && connectedSet.containsAll(sagaSet));
    }

    private TypeLevelCoverage typeLevelCoverage(Map<String, SagaDefinition> sagaByFqn,
                                                 Map<String, List<InputVariant>> inputsBySaga,
                                                 GraphViews graphViews,
                                                 ScenarioGeneratorConfig config) {
        List<String> discovered = sagaByFqn.keySet().stream().sorted().toList();
        List<String> withInputs = discovered.stream()
                .filter(sagaFqn -> !inputsBySaga.getOrDefault(sagaFqn, List.of()).isEmpty())
                .toList();
        List<String> withoutInputs = discovered.stream()
                .filter(sagaFqn -> inputsBySaga.getOrDefault(sagaFqn, List.of()).isEmpty())
                .toList();
        return new TypeLevelCoverage(
                discovered,
                discovered.size(),
                withInputs,
                withoutInputs,
                interactionCoverage(discovered, inputsBySaga, graphViews.strict(), config),
                interactionCoverage(discovered, inputsBySaga, graphViews.broad(), config));
    }

    private ExecutorReadiness executorReadiness(Map<String, List<InputVariant>> inputsBySaga) {
        List<InputVariant> acceptedInputs = inputsBySaga.values().stream()
                .flatMap(List::stream)
                .toList();
        int materializable = 0;
        int staticRecipeReady = 0;
        LinkedHashMap<String, Integer> blockerCounts = new LinkedHashMap<>();
        LinkedHashMap<String, Integer> runtimeOwnedResolutionCounts = new LinkedHashMap<>();
        ScenarioExecutorReadinessEvaluator evaluator = new ScenarioExecutorReadinessEvaluator();
        for (InputVariant input : acceptedInputs) {
            ScenarioExecutorReadinessEvaluator.Readiness readiness = evaluator.evaluate(input);
            if (readiness.materializable()) {
                materializable++;
            }
            if (readiness.staticRecipeReady()) {
                staticRecipeReady++;
            }
            readiness.blockers().forEach(blocker -> blockerCounts.merge(blocker, 1, Integer::sum));
            readiness.runtimeOwnedResolutions().forEach(type -> runtimeOwnedResolutionCounts.merge(type, 1, Integer::sum));
        }
        LinkedHashMap<String, Integer> sortedBlockerCounts = new LinkedHashMap<>();
        blockerCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedBlockerCounts.put(entry.getKey(), entry.getValue()));
        LinkedHashMap<String, Integer> sortedRuntimeOwnedResolutionCounts = new LinkedHashMap<>();
        runtimeOwnedResolutionCounts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> sortedRuntimeOwnedResolutionCounts.put(entry.getKey(), entry.getValue()));
        return new ExecutorReadiness(
                acceptedInputs.size(),
                materializable,
                materializable,
                staticRecipeReady,
                acceptedInputs.size() - materializable,
                sortedBlockerCounts,
                sortedRuntimeOwnedResolutionCounts);
    }

    private InteractionCoverage interactionCoverage(List<String> discoveredSagaFqns,
                                                    Map<String, List<InputVariant>> inputsBySaga,
                                                    ConflictGraphBuilder.Result graph,
                                                    ScenarioGeneratorConfig config) {
        List<List<String>> pairs = graph.conflictCandidates().stream()
                .map(candidate -> sortedPair(candidate.leftSagaFqn(), candidate.rightSagaFqn()))
                .distinct()
                .sorted(Comparator.comparing(pair -> String.join("|", pair)))
                .toList();
        int inputCovered = 0;
        for (List<String> pair : pairs) {
            if (pair.stream().allMatch(sagaFqn -> !inputsBySaga.getOrDefault(sagaFqn, List.of()).isEmpty())) {
                inputCovered++;
            }
        }
        return new InteractionCoverage(
                pairs.size(),
                inputCovered,
                pairs.size() - inputCovered,
                connectedSetCounts(discoveredSagaFqns, graph.adjacency(), config.maxSagaSetSize()));
    }

    private Map<String, String> connectedSetCounts(List<String> sagaFqns,
                                                   Map<String, Set<String>> adjacency,
                                                   int maxSagaSetSize) {
        LinkedHashMap<String, BigInteger> counts = new LinkedHashMap<>();
        ConnectedSagaSetEnumerator.enumerate(sagaFqns, adjacency, maxSagaSetSize).connectedSagaSets()
                .forEach(set -> counts.merge(Integer.toString(set.size()), BigInteger.ONE, BigInteger::add));
        LinkedHashMap<String, String> serialized = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> serialized.put(entry.getKey(), entry.getValue().toString()));
        return serialized;
    }

    private GraphViews buildGraphViews(List<SagaDefinition> sagaDefinitions, ScenarioGeneratorConfig config) {
        ScenarioGeneratorConfig strictConfig = graphConfig(config, false);
        ScenarioGeneratorConfig broadConfig = graphConfig(config, true);
        return new GraphViews(
                ConflictGraphBuilder.build(sagaDefinitions, strictConfig),
                ConflictGraphBuilder.build(sagaDefinitions, broadConfig));
    }

    private ScenarioGeneratorConfig graphConfig(ScenarioGeneratorConfig config, boolean allowTypeOnlyFallback) {
        return new ScenarioGeneratorConfig(
                config.exportEnabled(),
                config.generationStrategy(),
                config.catalogWriteMode(),
                config.includeSingles(),
                config.maxSagaSetSize(),
                config.maxCatalogScenarios(),
                config.maxInputVariantsPerSaga(),
                config.maxSchedulesPerInputTuple(),
                allowTypeOnlyFallback,
                config.inputPolicy(),
                config.scheduleStrategy(),
                config.deterministicSeed(),
                config.maxGroupedSagaSetRows());
    }

    private List<String> sortedPair(String left, String right) {
        return java.util.stream.Stream.of(left, right).sorted().toList();
    }

    BigInteger countCompatibleInputTuples(List<String> sagaSet, Map<String, List<InputVariant>> inputsBySaga) {
        return InputTupleSelection.count(sagaSet, inputsBySaga, List.of(), List.of(),
                InputTupleSelection.Mode.ALL);
    }

    private BigInteger scheduleCountPerTuple(List<String> sagaSet,
                                              Map<String, SagaDefinition> sagaByFqn,
                                              ScenarioGeneratorConfig config,
                                              GraphViews graphViews) {
        List<SagaDefinition> sagas = sagaSet.stream()
                .map(sagaByFqn::get)
                .filter(Objects::nonNull)
                .toList();
        BigInteger count;
        if (config.scheduleStrategy() == ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING) {
            count = orderPreservingInterleavings(sagas.stream().map(saga -> saga.steps().size()).toList());
        } else if (config.scheduleStrategy() == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED) {
            ConflictGraphBuilder.Result graph = config.allowTypeOnlyFallback() ? graphViews.broad() : graphViews.strict();
            List<ConflictGraphBuilder.ConflictCandidate> selectedCandidates = graph.conflictCandidates().stream()
                    .filter(candidate -> sagaSet.contains(candidate.leftSagaFqn()) && sagaSet.contains(candidate.rightSagaFqn()))
                    .toList();
            count = segmentCompressedCount(sagas, selectedCandidates);
        } else {
            count = BigInteger.ONE;
        }
        int cap = Math.max(0, config.maxSchedulesPerInputTuple());
        if (cap == 0) {
            return BigInteger.ZERO;
        }
        BigInteger capValue = BigInteger.valueOf(cap);
        return count.min(capValue);
    }

    private BigInteger segmentCompressedCount(List<SagaDefinition> sagas, List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates) {
        List<Integer> anchorCounts = sagas.stream()
                .map(saga -> conflictAnchorCount(saga, conflictCandidates))
                .filter(count -> count > 0)
                .toList();
        if (anchorCounts.isEmpty()) {
            return BigInteger.ONE;
        }
        return orderPreservingInterleavings(anchorCounts);
    }

    private int conflictAnchorCount(SagaDefinition saga, List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates) {
        Set<String> sagaStepIds = saga.steps().stream()
                .map(step -> ScenarioIdGenerator.stepDefinitionId(saga.sagaFqn(), step))
                .collect(java.util.stream.Collectors.toSet());
        Set<String> anchorIds = new java.util.LinkedHashSet<>();
        for (ConflictGraphBuilder.ConflictCandidate candidate : conflictCandidates) {
            if (Objects.equals(candidate.leftSagaFqn(), saga.sagaFqn()) && sagaStepIds.contains(candidate.leftStepId())) {
                anchorIds.add(candidate.leftStepId());
            }
            if (Objects.equals(candidate.rightSagaFqn(), saga.sagaFqn()) && sagaStepIds.contains(candidate.rightStepId())) {
                anchorIds.add(candidate.rightStepId());
            }
        }
        return anchorIds.size();
    }

    private BigInteger orderPreservingInterleavings(List<Integer> stepCounts) {
        int totalSteps = stepCounts.stream().mapToInt(Integer::intValue).sum();
        BigInteger numerator = factorial(totalSteps);
        BigInteger denominator = BigInteger.ONE;
        for (Integer stepCount : stepCounts) {
            denominator = denominator.multiply(factorial(Math.max(0, stepCount)));
        }
        return numerator.divide(denominator);
    }

    private BigInteger factorial(int value) {
        BigInteger result = BigInteger.ONE;
        for (int i = 2; i <= value; i++) {
            result = result.multiply(BigInteger.valueOf(i));
        }
        return result;
    }

    private ScenarioSpaceTotals totals(List<CalculatedRow> rows, boolean selected) {
        LinkedHashMap<String, BigInteger> bySize = new LinkedHashMap<>();
        BigInteger total = BigInteger.ZERO;
        for (CalculatedRow calculated : rows) {
            GroupedSagaSetRow row = calculated.row();
            BigInteger rowCount = selected ? calculated.selectedShapeCount() : calculated.allShapeCount();
            total = total.add(rowCount);
            bySize.merge(Integer.toString(row.sagaSetSize()), rowCount, BigInteger::add);
        }
        LinkedHashMap<String, String> serializedBySize = new LinkedHashMap<>();
        bySize.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEach(entry -> serializedBySize.put(entry.getKey(), entry.getValue().toString()));
        return new ScenarioSpaceTotals(total.toString(), serializedBySize);
    }

    private List<TopContributor> topContributors(List<GroupedSagaSetRow> rows) {
        List<GroupedSagaSetRow> orderedRows = rows.stream()
                .sorted(Comparator
                        .comparing((GroupedSagaSetRow row) -> new BigInteger(row.scenarioShapeCount())).reversed()
                        .thenComparing(GroupedSagaSetRow::sagaSetKey))
                .limit(TOP_CONTRIBUTOR_LIMIT)
                .toList();
        List<TopContributor> contributors = new ArrayList<>();
        for (int index = 0; index < orderedRows.size(); index++) {
            GroupedSagaSetRow row = orderedRows.get(index);
            contributors.add(new TopContributor(index + 1, row.sagaSetKey(), row.scenarioShapeCount()));
        }
        return List.copyOf(contributors);
    }

    private void collectCombinations(List<String> values,
                                     int targetSize,
                                     int startIndex,
                                     List<String> current,
                                     List<List<String>> combinations,
                                     int threshold) {
        if (current.size() == targetSize) {
            if (combinations.size() >= threshold) {
                throw new IllegalStateException("scenario-space accounting grouped saga-set rows exceed maxGroupedSagaSetRows=" + threshold);
            }
            combinations.add(List.copyOf(current));
            return;
        }
        for (int index = startIndex; index < values.size(); index++) {
            current.add(values.get(index));
            collectCombinations(values, targetSize, index + 1, current, combinations, threshold);
            current.remove(current.size() - 1);
        }
    }

    private Map<String, SagaDefinition> indexSagas(List<SagaDefinition> sagas) {
        LinkedHashMap<String, SagaDefinition> indexed = new LinkedHashMap<>();
        List<SagaDefinition> safeSagas = sagas == null ? List.of() : sagas;
        safeSagas.stream()
                .filter(saga -> saga != null && saga.sagaFqn() != null)
                .sorted(Comparator.comparing(SagaDefinition::sagaFqn))
                .forEach(saga -> indexed.putIfAbsent(saga.sagaFqn(), saga));
        return indexed;
    }

    private Map<String, List<InputVariant>> acceptedInputsByKnownSaga(Map<String, List<InputVariant>> inputsBySaga,
                                                                       Map<String, SagaDefinition> sagaByFqn) {
        LinkedHashMap<String, List<InputVariant>> result = new LinkedHashMap<>();
        Map<String, List<InputVariant>> safeInputs = inputsBySaga == null ? Map.of() : inputsBySaga;
        safeInputs.keySet().stream()
                .filter(sagaByFqn::containsKey)
                .sorted()
                .forEach(sagaFqn -> result.put(sagaFqn, List.copyOf(safeInputs.getOrDefault(sagaFqn, List.of()))));
        return result;
    }

    private String sagaSetKey(List<String> sagaSet) {
        return String.join("|", sagaSet);
    }

    private record CalculatedRow(GroupedSagaSetRow row,
                                 BigInteger allShapeCount,
                                 BigInteger selectedShapeCount,
                                 SetupShapeCounts setupShapeCounts) {
    }

    private record SetupShapeCounts(BigInteger withSourceSetup,
                                    BigInteger withoutSetup,
                                    BigInteger blocked) {

        private static SetupShapeCounts empty() {
            return new SetupShapeCounts(BigInteger.ZERO, BigInteger.ZERO, BigInteger.ZERO);
        }
    }

    private record GraphViews(ConflictGraphBuilder.Result strict, ConflictGraphBuilder.Result broad) {
    }
}
