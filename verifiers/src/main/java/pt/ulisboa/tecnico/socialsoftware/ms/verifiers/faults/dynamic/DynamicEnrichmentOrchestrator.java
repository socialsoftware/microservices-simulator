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
        List<String> selectedTestClassFqns = selectedTestClassFqns(testClassFqns);
        List<ScenarioPlan> safeScenarioPlans = scenarioPlans == null ? List.of() : scenarioPlans;
        inputMapWriter.write(evidenceRoot.resolve(DynamicInputMapWriter.FILE_NAME), selectedTestClassFqns, safeScenarioPlans, generatedAt);

        List<String> arguments = commandArguments(config, selectedTestClassFqns, evidenceRoot, applicationName);
        ProcessRunner.ProcessCommand command = new ProcessRunner.ProcessCommand(
                applicationPath,
                arguments,
                Duration.ofSeconds(config.perTestTimeoutSeconds()));

        String startedAt = nowIso();
        long startedNanos = System.nanoTime();
        ProcessRunner.ProcessResult processResult;
        try {
            processResult = processRunner.run(command);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            processResult = new ProcessRunner.ProcessResult(-1, "", e.getMessage(), true);
        } catch (IOException e) {
            processResult = new ProcessRunner.ProcessResult(-1, "", e.getMessage(), false);
        }
        long durationMillis = Duration.ofNanos(System.nanoTime() - startedNanos).toMillis();
        String finishedAt = nowIso();
        Path outputLog = evidenceRoot.resolve("maven-output.log");
        Files.writeString(outputLog, processResult.stdout() + (processResult.stderr().isBlank() ? "" : System.lineSeparator() + processResult.stderr()));

        SurefireTestRunReporter.Result reportedRuns = new SurefireTestRunReporter(objectMapper).write(
                applicationPath.resolve("target/surefire-reports"),
                applicationPath,
                evidenceRoot,
                selectedTestClassFqns,
                processResult.timedOut());
        String batchStatus = batchStatus(processResult, reportedRuns.records());
        int exitCode = processResult.exitCode();
        boolean timedOut = processResult.timedOut();
        List<TestRunRecord> testRuns = reportedRuns.records().stream()
                .map(record -> new TestRunRecord(
                        record.testClassFqn(),
                        arguments,
                        command.workingDirectory().toString(),
                        startedAt,
                        finishedAt,
                        durationMillis,
                        exitCode,
                        record.status(),
                        evidenceRoot.toString(),
                        outputLog.toString(),
                        timedOut,
                        record.reportPath(),
                        record.tests(),
                        record.failures(),
                        record.errors(),
                        record.skipped(),
                        record.warning(),
                        staticCatalogPath.toString()))
                .toList();
        writeBatchTestRunArtifacts(evidenceRoot, selectedTestClassFqns, arguments, command.workingDirectory().toString(),
                startedAt, finishedAt, durationMillis, processResult, batchStatus, staticCatalogPath, outputLog,
                reportedRuns.statusCounts(), reportedRuns.warnings(), testRuns);
        boolean hasFailure = processResult.timedOut()
                || processResult.exitCode() != 0
                || testRuns.stream().anyMatch(record -> "FAILED".equals(record.status()) || "TIMED_OUT".equals(record.status()) || "NO_REPORT".equals(record.status()));

        long readJoinWriteStartedNanos = System.nanoTime();
        DynamicEvidenceReadResult readResult = evidenceReader.read(evidenceRoot);
        DynamicEvidenceJoinResult joinResult = joiner.join(
                safeScenarioPlans,
                readResult.events(),
                readResult.evidenceFilesRead(),
                readResult.warnings(),
                readResult.evidenceBytesRead());
        long readJoinWriteDurationMillis = Duration.ofNanos(System.nanoTime() - readJoinWriteStartedNanos).toMillis();
        String dynamicRunFinishedAt = nowIso();

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
                generatedAt,
                reportMetadata(
                        startedAt,
                        dynamicRunFinishedAt,
                        durationMillis,
                        readJoinWriteDurationMillis,
                        batchStatus,
                        selectedTestClassFqns,
                        arguments,
                        staticCatalogPath,
                        evidenceRoot,
                        enrichedCatalogPath,
                        outputLog));

        Result result = new Result(testRuns, joinResult, evidenceRoot, enrichedCatalogPath, enrichedManifestPath, joinReportPath);
        if (hasFailure && !config.allowPartialTestRun()) {
            throw new IllegalStateException("Dynamic enrichment test run failed; artifacts were preserved under " + runDirectory);
        }
        return result;
    }

    private List<String> commandArguments(DynamicEnrichmentConfig config,
                                          List<String> testClassFqns,
                                          Path evidenceDir,
                                          String applicationName) {
        List<String> arguments = new ArrayList<>();
        arguments.add(config.maven().executable());
        arguments.add("-P" + config.maven().profile());
        arguments.add("test");
        arguments.add("-Dtest=" + String.join(",", testClassFqns));
        arguments.add("-Dspring.test.context.cache.maxSize=1");
        arguments.add("-Dsimulator.dynamic-evidence.enabled=true");
        arguments.add("-Dsimulator.dynamic-evidence.test-context.enabled=true");
        arguments.add("-Djunit.platform.listeners.autodetection.enabled=true");
        arguments.add("-Dsimulator.dynamic-evidence.output-dir=" + evidenceDir);
        arguments.add("-Dsimulator.dynamic-evidence.input-map-path=" + evidenceDir.resolve(DynamicInputMapWriter.FILE_NAME));
        arguments.add("-Dsimulator.dynamic-evidence.application-name=" + applicationName);
        return List.copyOf(arguments);
    }

    private Map<String, Object> reportMetadata(String dynamicRunStartedAt,
                                               String dynamicRunFinishedAt,
                                               long mavenDurationMillis,
                                               long readJoinWriteDurationMillis,
                                               String batchStatus,
                                               List<String> selectedTestClassFqns,
                                               List<String> commandArguments,
                                               Path staticCatalogPath,
                                               Path evidenceRoot,
                                               Path enrichedCatalogPath,
                                               Path outputLog) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("dynamicRunStartedAt", dynamicRunStartedAt);
        metadata.put("dynamicRunFinishedAt", dynamicRunFinishedAt);
        metadata.put("mavenDurationMillis", mavenDurationMillis);
        metadata.put("readJoinWriteDurationMillis", readJoinWriteDurationMillis);
        metadata.put("batchStatus", batchStatus);
        metadata.put("selectedTestClassFqns", selectedTestClassFqns);
        metadata.put("commandArguments", commandArguments);
        metadata.put("staticCatalogPath", staticCatalogPath.toString());
        metadata.put("dynamicEvidenceRoot", evidenceRoot.toString());
        metadata.put("enrichedCatalogPath", enrichedCatalogPath.toString());
        metadata.put("mavenOutputLogPath", outputLog.toString());
        return metadata;
    }

    private void writeBatchTestRunArtifacts(Path evidenceRoot,
                                            List<String> selectedTestClassFqns,
                                            List<String> arguments,
                                            String workingDirectory,
                                            String startedAt,
                                            String finishedAt,
                                            long durationMillis,
                                            ProcessRunner.ProcessResult processResult,
                                            String batchStatus,
                                            Path staticCatalogPath,
                                            Path outputLog,
                                            Map<String, Integer> statusCounts,
                                            List<String> warnings,
                                            List<TestRunRecord> testRuns) throws IOException {
        Map<String, Object> batch = new LinkedHashMap<>();
        batch.put("selectedTestClassFqns", selectedTestClassFqns);
        batch.put("commandArguments", arguments);
        batch.put("workingDirectory", workingDirectory);
        batch.put("startedAt", startedAt);
        batch.put("finishedAt", finishedAt);
        batch.put("durationMillis", durationMillis);
        batch.put("exitCode", processResult.exitCode());
        batch.put("status", batchStatus);
        batch.put("timedOut", processResult.timedOut());
        batch.put("staticCatalogPath", staticCatalogPath.toString());
        batch.put("evidenceRoot", evidenceRoot.toString());
        batch.put("outputLogPath", outputLog.toString());
        batch.put("statusCounts", statusCounts);
        batch.put("warnings", warnings);
        batch.put("testRuns", testRuns.stream().map(TestRunRecord::toMap).toList());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(evidenceRoot.resolve(SurefireTestRunReporter.TEST_RUN_FILE_NAME).toFile(), batch);

        Path sidecarDirectory = evidenceRoot.resolve(SurefireTestRunReporter.TEST_RUNS_DIRECTORY);
        Files.createDirectories(sidecarDirectory);
        for (TestRunRecord testRun : testRuns) {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(sidecarDirectory.resolve(safeTestClassDirectoryName(testRun.testClassFqn()) + ".json").toFile(), testRun.toMap());
        }
    }

    private String batchStatus(ProcessRunner.ProcessResult processResult, List<SurefireTestRunReporter.TestRunRecord> records) {
        if (processResult.timedOut()) {
            return "TIMED_OUT";
        }
        if (processResult.exitCode() != 0 || records.stream().anyMatch(record -> "FAILED".equals(record.status()) || "NO_REPORT".equals(record.status()))) {
            return "FAILED";
        }
        return "PASSED";
    }

    private List<String> selectedTestClassFqns(List<String> testClassFqns) {
        return (testClassFqns == null ? List.<String>of() : testClassFqns).stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .sorted()
                .toList();
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
                                 boolean timedOut,
                                 String reportPath,
                                 int tests,
                                 int failures,
                                 int errors,
                                 int skipped,
                                 String warning,
                                 String staticCatalogPath) {
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
            map.put("reportPath", reportPath);
            map.put("tests", tests);
            map.put("failures", failures);
            map.put("errors", errors);
            map.put("skipped", skipped);
            map.put("staticCatalogPath", staticCatalogPath);
            if (warning != null) {
                map.put("warning", warning);
            }
            return map;
        }
    }
}
