package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.nio.file.Path;

public record ScenarioExecutorOptions(
        Path runDirectory,
        Path catalogPath,
        Path outputPath,
        String scenarioId,
        boolean dryRun) {
}
