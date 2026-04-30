package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

public record ScenarioGeneratorConfig(
        boolean exportEnabled,
        boolean includeSingles,
        int maxSagaSetSize,
        int maxScenarios,
        int maxInputVariantsPerSaga,
        int maxSchedulesPerInputTuple,
        boolean allowTypeOnlyFallback,
        InputPolicy inputPolicy,
        ScheduleStrategy scheduleStrategy,
        long deterministicSeed) {

    public ScenarioGeneratorConfig() {
        this(false, true, 1, 100, 3, 20, false,
                InputPolicy.RESOLVED_OR_REPLAYABLE,
                ScheduleStrategy.SERIAL,
                1234L);
    }

    public ScenarioGeneratorConfig {
        inputPolicy = inputPolicy == null ? InputPolicy.RESOLVED_OR_REPLAYABLE : inputPolicy;
        scheduleStrategy = scheduleStrategy == null ? ScheduleStrategy.SERIAL : scheduleStrategy;
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
