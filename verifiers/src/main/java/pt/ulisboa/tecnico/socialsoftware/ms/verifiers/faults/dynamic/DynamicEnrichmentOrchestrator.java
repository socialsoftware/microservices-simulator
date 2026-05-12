package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export.EnrichedScenarioCatalogWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceReadResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DynamicEnrichmentOrchestrator {
    private final ProcessRunner processRunner;
    private final DynamicEvidenceReader evidenceReader;
    private final DynamicEvidenceJoiner joiner;
    private final EnrichedScenarioCatalogWriter writer;
    private final DynamicInputMapWriter inputMapWriter;
    private final ObjectMapper objectMapper;

    public DynamicEnrichmentOrchestrator() {
        this(new DefaultProcessRunner());
    }

    public DynamicEnrichmentOrchestrator(ProcessRunner processRunner) {
        this(processRunner, new DynamicEvidenceReader(), new DynamicEvidenceJoiner(), new EnrichedScenarioCatalogWriter(), new DynamicInputMapWriter(), new ObjectMapper());
    }

    public DynamicEnrichmentOrchestrator(ProcessRunner processRunner,
                                         DynamicEvidenceReader evidenceReader,
                                         DynamicEvidenceJoiner joiner,
                                         EnrichedScenarioCatalogWriter writer,
                                         ObjectMapper objectMapper) {
        this(processRunner, evidenceReader, joiner, writer, new DynamicInputMapWriter(objectMapper), objectMapper);
    }

    public DynamicEnrichmentOrchestrator(ProcessRunner processRunner,
                                         DynamicEvidenceReader evidenceReader,
                                         DynamicEvidenceJoiner joiner,
                                         EnrichedScenarioCatalogWriter writer,
                                         DynamicInputMapWriter inputMapWriter,
                                         ObjectMapper objectMapper) {
        this.processRunner = Objects.requireNonNull(processRunner, "processRunner cannot be null");
        this.evidenceReader = Objects.requireNonNull(evidenceReader, "evidenceReader cannot be null");
        this.joiner = Objects.requireNonNull(joiner, "joiner cannot be null");
        this.writer = Objects.requireNonNull(writer, "writer cannot be null");
        this.inputMapWriter = Objects.requireNonNull(inputMapWriter, "inputMapWriter cannot be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper cannot be null")
                .copy()
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public Result run(DynamicEnrichmentConfig config,
                      Path applicationPath,
                      String applicationName,
                      Path runDirectory,
                      List<String> testClassFqns,
                      List<ScenarioPlan> scenarioPlans,
                      Path staticCatalogPath,
                      String generatedAt) throws IOException {
        Objects.requireNonNull(config, "config cannot be null");
        Objects.requireNonNull(applicationPath, "applicationPath cannot be null");
        Objects.requireNonNull(runDirectory, "runDirectory cannot be null");
        Objects.requireNonNull(staticCatalogPath, "staticCatalogPath cannot be null");

        Path evidenceRoot = resolveRunRelativePath(runDirectory, config.dynamicEvidenceSubdir(), "dynamic-evidence");
        Files.createDirectories(evidenceRoot);
        List<TestRunRecord> testRuns = new ArrayList<>();
        boolean hasFailure = false;

        for (String testClassFqn : testClassFqns == null ? List.<String>of() : testClassFqns) {
            TestRunRecord record = runOne(config, applicationPath, applicationName, evidenceRoot, testClassFqn,
                    scenarioPlans == null ? List.of() : scenarioPlans, generatedAt);
            testRuns.add(record);
            if ("FAILED".equals(record.status()) || "TIMED_OUT".equals(record.status())) {
                hasFailure = true;
            }
        }

        DynamicEvidenceReadResult readResult = evidenceReader.read(evidenceRoot);
        DynamicEvidenceJoinResult joinResult = joiner.join(
                scenarioPlans == null ? List.of() : scenarioPlans,
                readResult.events(),
                readResult.evidenceFilesRead(),
                readResult.warnings());

        Path enrichedCatalogPath = resolveRunRelativePath(runDirectory, config.enrichedCatalogPath(), "scenario-catalog-enriched.jsonl");
        Path enrichedManifestPath = resolveRunRelativePath(runDirectory, config.enrichedManifestPath(), "scenario-catalog-enriched-manifest.json");
        Path joinReportPath = resolveRunRelativePath(runDirectory, config.joinReportPath(), "dynamic-evidence-join-report.json");
        writer.write(
                joinResult,
                enrichedCatalogPath,
                enrichedManifestPath,
                joinReportPath,
                staticCatalogPath.toString(),
                evidenceRoot.toString(),
                effectiveConfig(config),
                testRuns.stream().map(TestRunRecord::toMap).toList(),
                generatedAt);

        Result result = new Result(testRuns, joinResult, evidenceRoot, enrichedCatalogPath, enrichedManifestPath, joinReportPath);
        if (hasFailure && !config.allowPartialTestRun()) {
            throw new IllegalStateException("Dynamic enrichment test run failed; artifacts were preserved under " + runDirectory);
        }
        return result;
    }

    private TestRunRecord runOne(DynamicEnrichmentConfig config,
                                 Path applicationPath,
                                 String applicationName,
                                 Path evidenceRoot,
                                 String testClassFqn,
                                 List<ScenarioPlan> scenarioPlans,
                                 String generatedAt) throws IOException {
        Path evidenceDir = evidenceRoot.resolve(safeTestClassDirectoryName(testClassFqn)).normalize();
        Files.createDirectories(evidenceDir);
        inputMapWriter.write(evidenceDir.resolve(DynamicInputMapWriter.FILE_NAME), testClassFqn, scenarioPlans, generatedAt);
        List<String> arguments = commandArguments(config, testClassFqn, evidenceDir, applicationName);
        ProcessRunner.ProcessCommand command = new ProcessRunner.ProcessCommand(
                applicationPath,
                arguments,
                Duration.ofSeconds(config.perTestTimeoutSeconds()));

        String startedAt = nowIso();
        long startedNanos = System.nanoTime();
        ProcessRunner.ProcessResult result;
        try {
            result = processRunner.run(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result = new ProcessRunner.ProcessResult(-1, "", e.getMessage(), true);
        } catch (IOException e) {
            result = new ProcessRunner.ProcessResult(-1, "", e.getMessage(), false);
        }
        long durationMillis = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
        String finishedAt = nowIso();
        String status = result.timedOut() ? "TIMED_OUT" : result.exitCode() == 0 ? "PASSED" : "FAILED";
        Path outputLog = evidenceDir.resolve("maven-output.log");
        Files.writeString(outputLog, result.stdout() + (result.stderr().isBlank() ? "" : System.lineSeparator() + result.stderr()));
        TestRunRecord record = new TestRunRecord(
                testClassFqn,
                arguments,
                command.workingDirectory().toString(),
                startedAt,
                finishedAt,
                durationMillis,
                result.exitCode(),
                status,
                evidenceDir.toString(),
                outputLog.toString(),
                result.timedOut());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(evidenceDir.resolve("test-run.json").toFile(), record.toMap());
        return record;
    }

    private List<String> commandArguments(DynamicEnrichmentConfig config,
                                          String testClassFqn,
                                          Path evidenceDir,
                                          String applicationName) {
        List<String> arguments = new ArrayList<>();
        arguments.add(config.maven().executable());
        arguments.add("-P" + config.maven().profile());
        arguments.add("test");
        arguments.add("-Dtest=" + testClassFqn);
        arguments.add("-Dsimulator.dynamic-evidence.enabled=true");
        arguments.add("-Dsimulator.dynamic-evidence.test-context.enabled=true");
        arguments.add("-Djunit.platform.listeners.autodetection.enabled=true");
        arguments.add("-Dsimulator.dynamic-evidence.output-dir=" + evidenceDir);
        arguments.add("-Dsimulator.dynamic-evidence.application-name=" + applicationName);
        return List.copyOf(arguments);
    }

    private Map<String, Object> effectiveConfig(DynamicEnrichmentConfig config) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("enabled", config.enabled());
        map.put("allowPartialTestRun", config.allowPartialTestRun());
        map.put("dynamicEvidenceSubdir", config.dynamicEvidenceSubdir());
        map.put("enrichedCatalogPath", config.enrichedCatalogPath());
        map.put("enrichedManifestPath", config.enrichedManifestPath());
        map.put("joinReportPath", config.joinReportPath());
        map.put("testSourceRoot", config.testSourceRoot());
        map.put("includeTestDirs", config.includeTestDirs());
        map.put("excludeTestDirs", config.excludeTestDirs());
        map.put("excludeTestClasses", config.excludeTestClasses());
        map.put("perTestTimeoutSeconds", config.perTestTimeoutSeconds());
        map.put("mavenExecutable", config.maven().executable());
        map.put("mavenProfile", config.maven().profile());
        return map;
    }

    public static String safeTestClassDirectoryName(String testClassFqn) {
        String sanitized = (testClassFqn == null ? "test-class" : testClassFqn)
                .replaceAll("[^A-Za-z0-9._-]+", "-")
                .replaceAll("-+", "-")
                .replaceAll("(^-|-$)", "");
        return sanitized.isBlank() ? "test-class" : sanitized;
    }

    private static String nowIso() {
        return OffsetDateTime.now(ZoneOffset.UTC).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private Path resolveRunRelativePath(Path baseDirectory, String configuredPath, String defaultFileName) {
        Path safeBaseDirectory = baseDirectory.toAbsolutePath().normalize();
        if (configuredPath == null || configuredPath.isBlank()) {
            return safeBaseDirectory.resolve(defaultFileName).normalize();
        }

        Path configured = Path.of(configuredPath);
        if (configured.isAbsolute()) {
            throw new IllegalArgumentException("Configured output path must be relative to the verifier run directory: " + configuredPath);
        }

        Path resolved = safeBaseDirectory.resolve(configured).normalize();
        if (!resolved.startsWith(safeBaseDirectory)) {
            throw new IllegalArgumentException("Configured relative output path must stay under verifier run directory: " + configuredPath);
        }
        return resolved;
    }

    public record Result(List<TestRunRecord> testRuns,
                         DynamicEvidenceJoinResult joinResult,
                         Path evidenceRoot,
                         Path enrichedCatalogPath,
                         Path enrichedManifestPath,
                         Path joinReportPath) {
        public Result {
            testRuns = testRuns == null ? List.of() : List.copyOf(testRuns);
        }
    }

    public record TestRunRecord(String testClassFqn,
                                List<String> commandArguments,
                                String workingDirectory,
                                String startedAt,
                                String finishedAt,
                                long durationMillis,
                                int exitCode,
                                String status,
                                String evidenceDirectory,
                                String outputLogPath,
                                boolean timedOut) {
        public Map<String, Object> toMap() {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("testClassFqn", testClassFqn);
            map.put("commandArguments", commandArguments == null ? List.of() : commandArguments);
            map.put("workingDirectory", workingDirectory);
            map.put("startedAt", startedAt);
            map.put("finishedAt", finishedAt);
            map.put("durationMillis", durationMillis);
            map.put("exitCode", exitCode);
            map.put("status", status);
            map.put("evidenceDirectory", evidenceDirectory);
            map.put("outputLogPath", outputLogPath);
            map.put("timedOut", timedOut);
            return map;
        }
    }
}
