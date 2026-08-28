package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Runs source-derived setup preflight workloads behind independent JVM/Spring/H2 boundaries. */
final class ScenarioSetupPreflightProcessOrchestrator {
    static final Duration WORKER_TIMEOUT = Duration.ofMinutes(5);
    static final Duration TERMINATION_TIMEOUT = Duration.ofSeconds(5);

    private final WorkerProcessStarter processStarter;
    private final ObjectMapper mapper;
    private final Duration workerTimeout;
    private final Duration terminationTimeout;

    ScenarioSetupPreflightProcessOrchestrator() {
        this((command, workingDirectory) -> new ProcessBuilder(command)
                        .directory(workingDirectory == null ? null : workingDirectory.toFile())
                        .inheritIO()
                        .start(),
                new ObjectMapper(), WORKER_TIMEOUT, TERMINATION_TIMEOUT);
    }

    ScenarioSetupPreflightProcessOrchestrator(WorkerProcessStarter processStarter,
                                               ObjectMapper mapper,
                                               Duration workerTimeout,
                                               Duration terminationTimeout) {
        this.processStarter = processStarter;
        this.mapper = mapper;
        this.workerTimeout = positive(workerTimeout, "worker timeout");
        this.terminationTimeout = positive(terminationTimeout, "worker termination timeout");
    }

    int run(String[] originalArgs,
            ScenarioSetupPreflightOptions options,
            ScenarioExecutor.PreflightPlan plan) {
        long started = System.nanoTime();
        Path launchDirectory = Path.of("").toAbsolutePath().normalize();
        Path packagePath = options.packagePath().toAbsolutePath().normalize();
        Path expectedManifestPath = manifestPath(packagePath);
        Path outputPath = options.outputPath().toAbsolutePath().normalize();
        Path workingDirectory = applicationDirectory(options.applicationBase(), launchDirectory);
        List<AttemptOutcome> outcomes = new ArrayList<>();
        Path temporaryDirectory;
        try {
            Path outputParent = outputPath.getParent();
            if (outputParent != null) Files.createDirectories(outputParent);
            temporaryDirectory = Files.createTempDirectory(outputParent, ".setup-preflight-workers-");
        } catch (IOException failure) {
            throw new IllegalStateException("Cannot create isolated preflight report directory before setup", failure);
        }

        try {
            int attemptIndex = 0;
            for (Set<String> attempt : attempts(plan)) {
                Path workerOutput = temporaryDirectory.resolve("attempt-" + attemptIndex++ + ".json");
                AttemptOutcome outcome = new AttemptOutcome(attempt);
                try {
                    int status = runWorker(workerCommand(originalArgs, packagePath, workerOutput, attempt,
                            launchDirectory), workingDirectory);
                    if (status != 0) {
                        throw new IllegalStateException("isolated worker exited with nonzero status " + status);
                    }
                    if (!Files.isRegularFile(workerOutput)) {
                        throw new IllegalStateException("isolated worker exited without a report");
                    }
                    ScenarioSetupPreflightReport worker = readWorkerReport(workerOutput);
                    validateWorkerReport(worker, attempt, plan.sourceSetupWorkloadIds(), expectedManifestPath,
                            options);
                    outcome.accept(worker);
                } catch (Throwable failure) {
                    outcome.fail(failure);
                } finally {
                    try {
                        Files.deleteIfExists(workerOutput);
                    } catch (Throwable cleanupFailure) {
                        outcome.fail(cleanupFailure);
                    }
                }
                outcomes.add(outcome);
            }
        } finally {
            try {
                deleteRecursively(temporaryDirectory);
            } catch (Throwable cleanupFailure) {
                if (outcomes.isEmpty()) {
                    throw cleanupFailure instanceof RuntimeException runtime
                            ? runtime
                            : new IllegalStateException("Failed to clean isolated preflight worker directory", cleanupFailure);
                }
                outcomes.forEach(outcome -> outcome.fail(cleanupFailure));
            }
        }

        List<ScenarioSetupPreflightReport.WorkloadResult> workloads = new ArrayList<>();
        int participantCount = 0;
        for (AttemptOutcome outcome : outcomes) {
            if (outcome.failure != null) {
                workloads.addAll(isolationFailures(outcome.workloadIds, outcome.failure));
            } else {
                workloads.addAll(outcome.workloads);
                participantCount += outcome.participantCount;
            }
        }
        workloads.sort(Comparator.comparing(ScenarioSetupPreflightReport.WorkloadResult::workloadPlanId));
        String terminalStatus = workloads.size() == plan.candidateWorkloadIds().size()
                && workloads.stream().allMatch(workload -> "SETUP_READY".equals(workload.status()))
                ? "SUCCESS" : "SETUP_FAILED";
        ScenarioExecutionReport.RuntimeMetadata metadata = new ScenarioExecutionReport.RuntimeMetadata(
                options.applicationBase(), options.applicationId(), options.springApplicationClass(),
                options.springProfiles(), options.mavenProfile(), expectedManifestPath.toString(), null,
                "STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT", false);
        ScenarioSetupPreflightReport report = new ScenarioSetupPreflightReport(
                ScenarioSetupPreflightReport.SCHEMA_VERSION, UUID.randomUUID().toString(), terminalStatus,
                expectedManifestPath.toString(), ScenarioSetupPreflightReport.CANDIDATE_SELECTION,
                plan.candidateWorkloadIds().size(), participantCount, System.nanoTime() - started,
                metadata, workloads);
        try {
            mapper.writerWithDefaultPrettyPrinter().writeValue(outputPath.toFile(), report);
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to write setup preflight report " + outputPath, failure);
        }
        return report.successful() ? 0 : 1;
    }

    private int runWorker(List<String> command, Path workingDirectory) throws Exception {
        Process process = processStarter.start(command, workingDirectory);
        try {
            if (!process.waitFor(workerTimeout.toNanos(), TimeUnit.NANOSECONDS)) {
                TimeoutException timeout = new TimeoutException(
                        "isolated preflight worker exceeded " + workerTimeout);
                terminate(process, timeout, false);
                throw timeout;
            }
            return process.exitValue();
        } catch (InterruptedException interrupted) {
            terminate(process, interrupted, true);
            throw interrupted;
        } catch (Exception failure) {
            terminate(process, failure, false);
            throw failure;
        } catch (Error failure) {
            terminate(process, failure, false);
            throw failure;
        }
    }

    private void terminate(Process process, Throwable primary, boolean restoreInterrupt) {
        boolean interrupted = restoreInterrupt;
        try {
            if (!process.isAlive()) return;
            process.destroy();
            interrupted |= awaitTermination(process, terminationTimeout);
            if (process.isAlive()) {
                process.destroyForcibly();
                interrupted |= awaitTermination(process, terminationTimeout);
            }
            if (process.isAlive()) {
                primary.addSuppressed(new IllegalStateException(
                        "isolated preflight worker remained alive after forced termination"));
            }
        } finally {
            if (interrupted) Thread.currentThread().interrupt();
        }
    }

    /** Returns whether an interrupt was consumed while completing bounded termination. */
    private boolean awaitTermination(Process process, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        boolean interrupted = false;
        while (process.isAlive()) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) break;
            try {
                process.waitFor(remaining, TimeUnit.NANOSECONDS);
            } catch (InterruptedException ignored) {
                interrupted = true;
            }
        }
        return interrupted;
    }

    private ScenarioSetupPreflightReport readWorkerReport(Path workerOutput) throws IOException {
        JsonNode envelope = mapper.readTree(workerOutput.toFile());
        require(envelope != null && envelope.isObject(), "worker report envelope is not an object");
        require(envelope.path("schemaVersion").isTextual()
                        && ScenarioSetupPreflightReport.SCHEMA_VERSION.equals(
                        envelope.path("schemaVersion").textValue()),
                "unexpected worker report schema " + envelope.path("schemaVersion"));
        require(envelope.path("preflightAttemptId").isTextual(),
                "worker report attempt id is missing or malformed");
        require(envelope.path("terminalStatus").isTextual(),
                "worker report terminal status is missing or malformed");
        require(envelope.path("packageManifestPath").isTextual(),
                "worker report package path is missing or malformed");
        require(envelope.path("candidateSelection").isTextual(),
                "worker report candidate selection is missing or malformed");
        require(envelope.path("candidateCount").isIntegralNumber()
                        && envelope.path("candidateCount").canConvertToInt(),
                "worker report candidate count is missing or malformed");
        require(envelope.path("participantCount").isIntegralNumber()
                        && envelope.path("participantCount").canConvertToInt(),
                "worker report participant count is missing or malformed");
        require(envelope.path("setupDurationNanos").isIntegralNumber()
                        && envelope.path("setupDurationNanos").canConvertToLong(),
                "worker report duration is missing or malformed");
        require(envelope.path("runtimeMetadata").isObject(),
                "worker report runtime metadata is missing or malformed");
        require(envelope.path("workloads").isArray(),
                "worker report workloads are missing or malformed");
        for (JsonNode workload : envelope.path("workloads")) {
            require(workload.isObject(), "worker report contains a malformed workload result");
        }
        return mapper.treeToValue(envelope, ScenarioSetupPreflightReport.class);
    }

    private void validateWorkerReport(ScenarioSetupPreflightReport worker,
                                      Set<String> expectedWorkloads,
                                      Set<String> sourceSetupWorkloads,
                                      Path expectedManifestPath,
                                      ScenarioSetupPreflightOptions options) {
        require(ScenarioSetupPreflightReport.SCHEMA_VERSION.equals(worker.schemaVersion()),
                "unexpected worker report schema " + worker.schemaVersion());
        require("SUCCESS".equals(worker.terminalStatus()),
                "worker report is not terminal SUCCESS");
        require(ScenarioSetupPreflightReport.CANDIDATE_SELECTION.equals(worker.candidateSelection()),
                "unexpected worker candidate selection " + worker.candidateSelection());
        require(expectedManifestPath.toString().equals(worker.packageManifestPath()),
                "worker reported a different package path " + worker.packageManifestPath());
        require(worker.preflightAttemptId() != null && !worker.preflightAttemptId().isBlank(),
                "worker report has no attempt id");
        require(worker.setupDurationNanos() >= 0, "worker reported a negative duration");
        require(worker.candidateCount() == expectedWorkloads.size(),
                "worker reported candidate count " + worker.candidateCount());
        require(worker.workloads().size() == expectedWorkloads.size(),
                "worker reported workload result count " + worker.workloads().size());
        Set<String> reported = worker.workloads().stream()
                .map(ScenarioSetupPreflightReport.WorkloadResult::workloadPlanId)
                .collect(Collectors.toSet());
        require(reported.size() == worker.workloads().size() && reported.equals(expectedWorkloads),
                "worker reported unexpected workloads " + reported);
        int resultParticipantCount = worker.workloads().stream()
                .mapToInt(workload -> workload.participants().size()).sum();
        require(worker.participantCount() == resultParticipantCount,
                "worker participant count is inconsistent with workload results");
        require(resultParticipantCount > 0, "worker reported no participant results");
        for (ScenarioSetupPreflightReport.WorkloadResult workload : worker.workloads()) {
            require(workload.setupDurationNanos() >= 0,
                    "worker workload reported a negative duration: " + workload.workloadPlanId());
            require("SETUP_READY".equals(workload.status()),
                    "worker workload is not SETUP_READY: " + workload.workloadPlanId());
            require(workload.blockers().isEmpty(),
                    "worker workload contains blockers: " + workload.workloadPlanId());
            require(!workload.participants().isEmpty(),
                    "worker workload has no participants: " + workload.workloadPlanId());
            for (ScenarioSetupPreflightReport.ParticipantResult participant : workload.participants()) {
                require(participant.setupReady()
                                && "MATERIALIZED".equals(participant.materializationState())
                                && "STARTUP_READY".equals(participant.startupState())
                                && participant.blockers().isEmpty(),
                        "worker participant result is inconsistent with SETUP_READY: "
                                + workload.workloadPlanId());
            }
            boolean sourceSetupExpected = sourceSetupWorkloads.contains(workload.workloadPlanId());
            require(sourceSetupExpected == (workload.sourceSetup() != null),
                    "worker source-setup evidence does not match workload selection: "
                            + workload.workloadPlanId());
            if (sourceSetupExpected) {
                require("SUCCEEDED".equals(workload.sourceSetup().status())
                                && workload.sourceSetup().pendingEventsCleared() >= 0
                                && workload.sourceSetup().emptyPendingEventBaseline()
                                && workload.sourceSetup().failureReason() == null
                                && workload.sourceSetup().failureMessage() == null
                                && !workload.sourceSetup().actions().isEmpty()
                                && workload.sourceSetup().actions().stream()
                                .allMatch(action -> "SUCCEEDED".equals(action.status()))
                                && !workload.sourceSetup().participantBindings().isEmpty()
                                && workload.sourceSetup().participantBindings().stream()
                                .allMatch(binding -> "RESOLVED".equals(binding.status())),
                        "worker source setup did not complete successfully: " + workload.workloadPlanId());
            }
        }
        ScenarioExecutionReport.RuntimeMetadata metadata = worker.runtimeMetadata();
        require(metadata != null
                        && Objects.equals(options.applicationBase(), metadata.applicationBase())
                        && Objects.equals(options.applicationId(), metadata.applicationId())
                        && Objects.equals(options.springApplicationClass(), metadata.springApplicationClass())
                        && Objects.equals(options.springProfiles(), metadata.springProfiles())
                        && Objects.equals(options.mavenProfile(), metadata.mavenProfile())
                        && expectedManifestPath.toString().equals(metadata.packageManifestPath())
                        && metadata.faultScenarioId() == null
                        && "STATIC_MATERIALIZABILITY_CANDIDATE_PREFLIGHT".equals(metadata.executorMode())
                        && !metadata.dryRun(),
                "worker runtime metadata is incompatible with the parent request");
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private List<Set<String>> attempts(ScenarioExecutor.PreflightPlan plan) {
        List<Set<String>> attempts = new ArrayList<>();
        Set<String> legacy = new LinkedHashSet<>(plan.candidateWorkloadIds());
        legacy.removeAll(plan.sourceSetupWorkloadIds());
        if (!legacy.isEmpty()) attempts.add(Set.copyOf(legacy));
        plan.sourceSetupWorkloadIds().stream().sorted()
                .forEach(id -> attempts.add(Set.of(id)));
        return attempts;
    }

    private List<String> workerCommand(String[] originalArgs,
                                       Path packagePath,
                                       Path output,
                                       Set<String> workloadIds,
                                       Path launchDirectory) {
        List<String> command = new ArrayList<>();
        command.add(Path.of(System.getProperty("java.home"), "bin", "java").toAbsolutePath().normalize().toString());
        command.add("-Dmicroservices.simulator.event-replay.enabled=true");
        command.add("-cp");
        command.add(normalizedClassPath(launchDirectory));
        command.add(ScenarioExecutorCli.class.getName());
        for (int index = 0; index < originalArgs.length; index++) {
            String argument = originalArgs[index];
            if ("--output-path".equals(argument) || "--package-path".equals(argument)) {
                index++;
                continue;
            }
            if ("--preflight-worker".equals(argument) || "--preflight-workload-ids".equals(argument)) {
                if (index + 1 < originalArgs.length && !originalArgs[index + 1].startsWith("--")) index++;
                continue;
            }
            command.add(argument);
        }
        command.add("--package-path");
        command.add(packagePath.toString());
        command.add("--output-path");
        command.add(output.toAbsolutePath().normalize().toString());
        command.add("--preflight-worker");
        command.add("--preflight-workload-ids");
        command.add(String.join(",", workloadIds.stream().sorted().toList()));
        return command;
    }

    private String normalizedClassPath(Path launchDirectory) {
        return Stream.of(System.getProperty("java.class.path").split(
                        java.util.regex.Pattern.quote(File.pathSeparator), -1))
                .map(entry -> entry.isEmpty() ? launchDirectory : Path.of(entry))
                .map(path -> path.isAbsolute() ? path.normalize() : launchDirectory.resolve(path).normalize())
                .map(Path::toString)
                .collect(Collectors.joining(File.pathSeparator));
    }

    private List<ScenarioSetupPreflightReport.WorkloadResult> isolationFailures(
            Set<String> workloadIds, Throwable failure) {
        String message = failureMessage(failure);
        return workloadIds.stream().sorted().map(id -> {
            ScenarioExecutionReport.Blocker blocker = new ScenarioExecutionReport.Blocker(
                    id, null, null, null, null, null,
                    "FRESH_STATE_ISOLATION_FAILED", message);
            return new ScenarioSetupPreflightReport.WorkloadResult(
                    id, "FRESH_STATE_ISOLATION_FAILED", 0L, null, List.of(), List.of(blocker));
        }).toList();
    }

    private String failureMessage(Throwable failure) {
        StringBuilder message = new StringBuilder(failure.getClass().getName());
        if (failure.getMessage() != null) message.append(": ").append(failure.getMessage());
        for (Throwable suppressed : failure.getSuppressed()) {
            message.append("; suppressed ").append(suppressed.getClass().getName());
            if (suppressed.getMessage() != null) message.append(": ").append(suppressed.getMessage());
        }
        return message.toString();
    }

    private Path applicationDirectory(String applicationBase, Path launchDirectory) {
        if (applicationBase == null || applicationBase.isBlank()) return null;
        Path configured = Path.of(applicationBase);
        Path path = configured.isAbsolute() ? configured.normalize() : launchDirectory.resolve(configured).normalize();
        return Files.isDirectory(path) ? path : null;
    }

    private Path manifestPath(Path configured) {
        return (Files.isDirectory(configured) ? configured.resolve("scenario-catalog-manifest.json") : configured)
                .toAbsolutePath().normalize();
    }

    private void deleteRecursively(Path path) {
        if (path == null || !Files.exists(path)) return;
        try (Stream<Path> paths = Files.walk(path)) {
            for (Path candidate : paths.sorted(Comparator.reverseOrder()).toList()) {
                Files.deleteIfExists(candidate);
            }
        } catch (IOException failure) {
            throw new IllegalStateException("Failed to clean isolated preflight worker path " + path, failure);
        }
    }

    private static Duration positive(Duration duration, String name) {
        if (duration == null || duration.isZero() || duration.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return duration;
    }

    interface WorkerProcessStarter {
        Process start(List<String> command, Path workingDirectory) throws IOException;
    }

    private static final class AttemptOutcome {
        private final Set<String> workloadIds;
        private List<ScenarioSetupPreflightReport.WorkloadResult> workloads = List.of();
        private int participantCount;
        private Throwable failure;

        private AttemptOutcome(Set<String> workloadIds) {
            this.workloadIds = workloadIds;
        }

        private void accept(ScenarioSetupPreflightReport report) {
            workloads = report.workloads();
            participantCount = report.participantCount();
        }

        private void fail(Throwable next) {
            if (failure == null) {
                failure = next;
            } else if (failure != next) {
                failure.addSuppressed(next);
            }
            workloads = List.of();
            participantCount = 0;
        }
    }
}
