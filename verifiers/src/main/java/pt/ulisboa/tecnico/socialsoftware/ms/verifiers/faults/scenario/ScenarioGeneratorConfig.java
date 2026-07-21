package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

public record ScenarioGeneratorConfig(
        boolean exportEnabled,
        GenerationStrategy generationStrategy,
        CatalogWriteMode catalogWriteMode,
        boolean includeSingles,
        int maxSagaSetSize,
        int maxCatalogScenarios,
        int maxInputVariantsPerSaga,
        int maxSchedulesPerInputTuple,
        boolean allowTypeOnlyFallback,
        InputPolicy inputPolicy,
        ScheduleStrategy scheduleStrategy,
        long deterministicSeed,
        int maxGroupedSagaSetRows) {

    public ScenarioGeneratorConfig(boolean exportEnabled,
                                   GenerationStrategy generationStrategy,
                                   CatalogWriteMode catalogWriteMode,
                                   boolean includeSingles,
                                   int maxSagaSetSize,
                                    int maxCatalogScenarios,
                                   int maxInputVariantsPerSaga,
                                   int maxSchedulesPerInputTuple,
                                   boolean allowTypeOnlyFallback,
                                   InputPolicy inputPolicy,
                                   ScheduleStrategy scheduleStrategy,
                                   long deterministicSeed) {
        this(exportEnabled,
                generationStrategy,
                catalogWriteMode,
                includeSingles,
                maxSagaSetSize,
                maxCatalogScenarios,
                maxInputVariantsPerSaga,
                maxSchedulesPerInputTuple,
                allowTypeOnlyFallback,
                inputPolicy,
                scheduleStrategy,
                deterministicSeed,
                100000);
    }

    public ScenarioGeneratorConfig() {
        this(false, GenerationStrategy.INTERACTION_PRUNED, CatalogWriteMode.WRITE_WORKLOADS, true, 1, 100, 3, 20, false,
                InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScheduleStrategy.SERIAL,
                1234L,
                100000);
    }

    public ScenarioGeneratorConfig {
        generationStrategy = generationStrategy == null ? GenerationStrategy.INTERACTION_PRUNED : generationStrategy;
        catalogWriteMode = catalogWriteMode == null ? CatalogWriteMode.WRITE_WORKLOADS : catalogWriteMode;
        inputPolicy = inputPolicy == null ? InputPolicy.RESOLVED_OR_REPLAYABLE : inputPolicy;
        scheduleStrategy = scheduleStrategy == null ? ScheduleStrategy.SERIAL : scheduleStrategy;
    }

    public enum GenerationStrategy {
        INTERACTION_PRUNED,
        BRUTE_FORCE
    }

    public enum CatalogWriteMode {
        WRITE_WORKLOADS,
        COUNT_ONLY
    }

    public enum InputPolicy {
        RESOLVED_ONLY,
        RESOLVED_OR_REPLAYABLE,
        ALLOW_PARTIAL,
        ALLOW_UNRESOLVED
    }

    public enum ScheduleStrategy {
        SERIAL,
        ORDER_PRESERVING_INTERLEAVING,
        SEGMENT_COMPRESSED
    }
}
