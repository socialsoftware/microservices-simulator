package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record ScenarioSpaceAccountingReport(
        String schemaVersion,
        AccountingRunConfig runConfig,
        TypeLevelCoverage typeLevelCoverage,
        ExecutorReadiness executorReadiness,
        InputBoundScenarioSpace inputBoundScenarioSpace,
        List<GroupedSagaSetRow> groupedSagaSets,
        List<TopContributor> topContributors) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-space-accounting.v1";

    public ScenarioSpaceAccountingReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        runConfig = runConfig == null ? AccountingRunConfig.from("unknown", new ScenarioGeneratorConfig()) : runConfig;
        typeLevelCoverage = typeLevelCoverage == null ? TypeLevelCoverage.empty() : typeLevelCoverage;
        executorReadiness = executorReadiness == null ? ExecutorReadiness.empty() : executorReadiness;
        inputBoundScenarioSpace = inputBoundScenarioSpace == null ? InputBoundScenarioSpace.empty() : inputBoundScenarioSpace;
        groupedSagaSets = groupedSagaSets == null ? List.of() : List.copyOf(groupedSagaSets);
        topContributors = topContributors == null ? List.of() : List.copyOf(topContributors);
    }

    public static ScenarioSpaceAccountingReport placeholder(String targetApplication,
                                                            ScenarioGeneratorConfig config,
                                                            int catalogWritten) {
        String written = Integer.toString(Math.max(0, catalogWritten));
        return new ScenarioSpaceAccountingReport(
                SCHEMA_VERSION,
                AccountingRunConfig.from(targetApplication, config),
                TypeLevelCoverage.empty(),
                ExecutorReadiness.empty(),
                new InputBoundScenarioSpace(
                        new ScenarioSpaceTotals("0", Map.of()),
                        new ScenarioSpaceTotals("0", Map.of()),
                        new ScenarioSpaceTotals(written, Map.of())),
                List.of(),
                List.of());
    }

    public record AccountingRunConfig(
            String targetApplication,
            ScenarioGeneratorConfig.GenerationStrategy generationStrategy,
            ScenarioGeneratorConfig.CatalogWriteMode catalogWriteMode,
            boolean includeSingles,
            int maxSagaSetSize,
            int maxInputVariantsPerSaga,
            int maxSchedulesPerInputTuple,
            int maxGroupedSagaSetRows,
            int maxCatalogScenarios,
            ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
            String effectiveSegmentBehavior,
            boolean allowTypeOnlyFallback,
            ScenarioGeneratorConfig.InputPolicy inputPolicy,
            String sourceModeHandling) {

        public static AccountingRunConfig from(String targetApplication, ScenarioGeneratorConfig config) {
            ScenarioGeneratorConfig safeConfig = config == null ? new ScenarioGeneratorConfig() : config;
            return new AccountingRunConfig(
                    targetApplication == null || targetApplication.isBlank() ? "unknown" : targetApplication,
                    safeConfig.generationStrategy(),
                    safeConfig.catalogWriteMode(),
                    safeConfig.includeSingles(),
                    safeConfig.maxSagaSetSize(),
                    safeConfig.maxInputVariantsPerSaga(),
                    safeConfig.maxSchedulesPerInputTuple(),
                    safeConfig.maxGroupedSagaSetRows(),
                    safeConfig.maxCatalogScenarios(),
                    safeConfig.scheduleStrategy(),
                    effectiveSegmentBehavior(safeConfig.scheduleStrategy()),
                    safeConfig.allowTypeOnlyFallback(),
                    safeConfig.inputPolicy(),
                    "SAGAS accepted; TCC and MIXED rejected; UNKNOWN accepted with warning");
        }

        private static String effectiveSegmentBehavior(ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy) {
            if (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED) {
                return "conflict-anchor segment compression: order-preserving interleavings over cross-saga conflict anchors, expanded to deterministic in-saga anchor segments";
            }
            return "not-applicable";
        }
    }

    public record InputBoundScenarioSpace(
            ScenarioSpaceTotals allInputBound,
            ScenarioSpaceTotals selectedByGenerator,
            ScenarioSpaceTotals catalogWritten) {

        public static InputBoundScenarioSpace empty() {
            return new InputBoundScenarioSpace(
                    new ScenarioSpaceTotals("0", Map.of()),
                    new ScenarioSpaceTotals("0", Map.of()),
                    new ScenarioSpaceTotals("0", Map.of()));
        }
    }

    public record ExecutorReadiness(
            int acceptedInputVariantCount,
            int executorMaterializableInputVariantCount,
            int executorReadyInputVariantCount,
            int staticRecipeReadyInputVariantCount,
            int blockedInputVariantCount,
            Map<String, Integer> blockerReasonCounts,
            Map<String, Integer> runtimeOwnedResolutionCounts) {

        public ExecutorReadiness {
            blockerReasonCounts = stableMap(blockerReasonCounts);
            runtimeOwnedResolutionCounts = stableMap(runtimeOwnedResolutionCounts);
        }

        public static ExecutorReadiness empty() {
            return new ExecutorReadiness(0, 0, 0, 0, 0, Map.of(), Map.of());
        }
    }

    public record TypeLevelCoverage(
            List<String> discoveredSagaFqns,
            int discoveredSagaCount,
            List<String> sagasWithAcceptedInputs,
            List<String> sagasWithoutAcceptedInputs,
            InteractionCoverage strict,
            InteractionCoverage broad) {

        public TypeLevelCoverage {
            discoveredSagaFqns = discoveredSagaFqns == null ? List.of() : List.copyOf(discoveredSagaFqns);
            sagasWithAcceptedInputs = sagasWithAcceptedInputs == null ? List.of() : List.copyOf(sagasWithAcceptedInputs);
            sagasWithoutAcceptedInputs = sagasWithoutAcceptedInputs == null ? List.of() : List.copyOf(sagasWithoutAcceptedInputs);
            strict = strict == null ? InteractionCoverage.empty() : strict;
            broad = broad == null ? InteractionCoverage.empty() : broad;
        }

        public static TypeLevelCoverage empty() {
            return new TypeLevelCoverage(List.of(), 0, List.of(), List.of(), InteractionCoverage.empty(), InteractionCoverage.empty());
        }
    }

    public record InteractionCoverage(
            int interactionPairCount,
            int inputCoveredInteractionPairCount,
            int missingInputInteractionPairCount,
            Map<String, String> connectedSetCountsBySize) {

        public InteractionCoverage {
            connectedSetCountsBySize = stableMap(connectedSetCountsBySize);
        }

        public static InteractionCoverage empty() {
            return new InteractionCoverage(0, 0, 0, Map.of());
        }
    }

    public record ScenarioSpaceTotals(String total, Map<String, String> bySagaSetSize) {
        public ScenarioSpaceTotals {
            total = total == null || total.isBlank() ? "0" : total;
            bySagaSetSize = stableMap(bySagaSetSize);
        }
    }

    public record GroupedSagaSetRow(
            String sagaSetKey,
            int sagaSetSize,
            List<String> sagaFqns,
            Map<String, Integer> inputCountsBySaga,
            Map<String, Integer> stepCountsBySaga,
            String compatibleInputTupleCount,
            String scheduleCountPerTuple,
            String scenarioShapeCount,
            InteractionSummary strictInteractionSummary,
            InteractionSummary broadInteractionSummary,
            boolean selectedByConfiguredGenerator) {

        public GroupedSagaSetRow {
            sagaSetKey = sagaSetKey == null || sagaSetKey.isBlank() ? "unknown" : sagaSetKey;
            sagaFqns = sagaFqns == null ? List.of() : List.copyOf(sagaFqns);
            inputCountsBySaga = stableMap(inputCountsBySaga);
            stepCountsBySaga = stableMap(stepCountsBySaga);
            compatibleInputTupleCount = compatibleInputTupleCount == null || compatibleInputTupleCount.isBlank() ? "0" : compatibleInputTupleCount;
            scheduleCountPerTuple = scheduleCountPerTuple == null || scheduleCountPerTuple.isBlank() ? "0" : scheduleCountPerTuple;
            scenarioShapeCount = scenarioShapeCount == null || scenarioShapeCount.isBlank() ? "0" : scenarioShapeCount;
            strictInteractionSummary = strictInteractionSummary == null ? InteractionSummary.placeholder() : strictInteractionSummary;
            broadInteractionSummary = broadInteractionSummary == null ? InteractionSummary.placeholder() : broadInteractionSummary;
        }
    }

    public record InteractionSummary(
            boolean connected,
            int directPairCount,
            Map<String, Integer> evidenceKindCounts) {

        public InteractionSummary {
            evidenceKindCounts = stableMap(evidenceKindCounts);
        }

        public static InteractionSummary placeholder() {
            return new InteractionSummary(false, 0, Map.of());
        }
    }

    public record TopContributor(
            int rank,
            String sagaSetKey,
            String representedScenarioShapeCount) {

        public TopContributor {
            sagaSetKey = sagaSetKey == null || sagaSetKey.isBlank() ? "unknown" : sagaSetKey;
            representedScenarioShapeCount = representedScenarioShapeCount == null || representedScenarioShapeCount.isBlank()
                    ? "0"
                    : representedScenarioShapeCount;
        }
    }

    private static <K, V> Map<K, V> stableMap(Map<K, V> values) {
        return values == null ? Map.of() : Collections.unmodifiableMap(new LinkedHashMap<>(values));
    }
}
