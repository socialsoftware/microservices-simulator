package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.nio.file.Path;

public record ScenarioExecutorOptions(
        Path packagePath,
        Path outputPath,
        String faultScenarioId,
        boolean dryRun,
        String applicationBase,
        String applicationId,
        String springApplicationClass,
        String springProfiles,
        String mavenProfile) {

    public ScenarioExecutorOptions(Path packagePath,
                                   Path outputPath,
                                   String faultScenarioId,
                                   boolean dryRun) {
        this(packagePath, outputPath, faultScenarioId, dryRun, null, null, null, null, null);
    }
}
