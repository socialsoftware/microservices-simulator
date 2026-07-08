package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScenarioExecutorOrchestrator {
    private final ProcessRunner processRunner;

    public ScenarioExecutorOrchestrator(ProcessRunner processRunner) {
        this.processRunner = processRunner;
    }

    public int run(Config config) {
        validate(config);
        int prepare = processRunner.run(List.of("mvn", "-P", config.mavenProfile(), "test-compile"), config.applicationBaseDirectory());
        if (prepare != 0) return prepare;
        List<String> command = new ArrayList<>();
        command.add("java");
        command.add("-cp");
        command.add(config.classpath());
        command.add("pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorCli");
        command.add("--spring-application-class");
        command.add(config.springApplicationClass());
        command.add("--spring-profiles");
        command.add(config.springProfiles());
        command.add("--application-base");
        command.add(config.applicationBaseDirectory().toString());
        command.add("--application-id");
        command.add(applicationId(config.applicationBaseDirectory()));
        command.add("--maven-profile");
        command.add(config.mavenProfile());
        command.add("--catalog-path");
        command.add(config.catalogPath().toString());
        command.add("--output-path");
        command.add(config.outputPath().toString());
        if (config.scenarioId() != null && !config.scenarioId().isBlank()) {
            command.add("--scenario-id");
            command.add(config.scenarioId());
        }
        if (config.faultVector() != null && !config.faultVector().isBlank()) {
            command.add("--fault-vector");
            command.add(config.faultVector());
        }
        return processRunner.run(command, config.applicationBaseDirectory());
    }

    private void validate(Config config) {
        if (config.applicationBaseDirectory() == null) throw new IllegalArgumentException("application base directory is required");
        if (blank(config.springApplicationClass())) throw new IllegalArgumentException("Spring application class is required");
        if (blank(config.mavenProfile())) throw new IllegalArgumentException("Maven profile is required");
        if (blank(config.springProfiles())) throw new IllegalArgumentException("Spring profiles are required");
        if (config.catalogPath() == null) throw new IllegalArgumentException("catalog path or run directory is required");
        if (config.outputPath() == null) throw new IllegalArgumentException("output path is required");
    }

    private boolean blank(String value) { return value == null || value.isBlank(); }

    private String applicationId(Path applicationBaseDirectory) {
        Path fileName = applicationBaseDirectory.getFileName();
        return fileName == null ? applicationBaseDirectory.toString() : fileName.toString();
    }

    public interface ProcessRunner {
        int run(List<String> command, Path workingDirectory);
    }

    public record Config(Path applicationBaseDirectory,
                         String springApplicationClass,
                         String mavenProfile,
                         String springProfiles,
                         Path catalogPath,
                         Path outputPath,
                         String scenarioId,
                         String faultVector,
                         String classpath) {
    }
}
