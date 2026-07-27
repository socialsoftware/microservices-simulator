package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.nio.file.Path;

public record ScenarioSetupPreflightOptions(
        Path packagePath,
        Path outputPath,
        String applicationBase,
        String applicationId,
        String springApplicationClass,
        String springProfiles,
        String mavenProfile) {

    public ScenarioSetupPreflightOptions(Path packagePath, Path outputPath) {
        this(packagePath, outputPath, null, null, null, null, null);
    }
}
