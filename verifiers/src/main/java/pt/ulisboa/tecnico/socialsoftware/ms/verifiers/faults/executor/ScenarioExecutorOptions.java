package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.nio.file.Path;

public record ScenarioExecutorOptions(
        Path runDirectory,
        Path catalogPath,
        Path outputPath,
        String scenarioId,
        String faultVector,
        boolean dryRun,
        String applicationBase,
        String applicationId,
        String springApplicationClass,
        String springProfiles,
        String mavenProfile) {
    public ScenarioExecutorOptions(Path runDirectory, Path catalogPath, Path outputPath, String scenarioId, boolean dryRun) {
        this(runDirectory, catalogPath, outputPath, scenarioId, null, dryRun);
    }

    public ScenarioExecutorOptions(Path runDirectory, Path catalogPath, Path outputPath, String scenarioId, String faultVector, boolean dryRun) {
        this(runDirectory, catalogPath, outputPath, scenarioId, faultVector, dryRun, null, null, null, null, null);
    }
}
