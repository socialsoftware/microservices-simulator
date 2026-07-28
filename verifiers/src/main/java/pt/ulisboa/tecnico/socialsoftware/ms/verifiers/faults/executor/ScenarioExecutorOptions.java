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
        String mavenProfile,
        Path impactOutputPath) {

    public ScenarioExecutorOptions(Path packagePath,
                                   Path outputPath,
                                   String faultScenarioId,
                                   boolean dryRun,
                                   String applicationBase,
                                   String applicationId,
                                   String springApplicationClass,
                                   String springProfiles,
                                   String mavenProfile) {
        this(packagePath, outputPath, faultScenarioId, dryRun, applicationBase, applicationId,
                springApplicationClass, springProfiles, mavenProfile, null);
    }

    public ScenarioExecutorOptions(Path packagePath,
                                   Path outputPath,
                                   String faultScenarioId,
                                   boolean dryRun) {
        this(packagePath, outputPath, faultScenarioId, dryRun, null, null, null, null, null, null);
    }
}
