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
    private static final Set<String> SOURCE_SETUP_FAILURE_REASONS = Set.of(
            "MIXED_PREREQUISITE_AND_SETUP", "DUPLICATE_SETUP_METHOD_DISPATCH", "DUPLICATE_SETUP_PARTICIPANT_BINDING",
            "INVALID_SETUP_RESULT_REFERENCE", "MALFORMED_SETUP_DISPATCHER", "MISSING_SETUP_RESULT",
            "MISSING_SETUP_RUNTIME_TYPE", "SETUP_CLEANUP_FAILED", "SETUP_DECLARED_TYPE_MISMATCH",
            "SETUP_DISPATCH_FAILED", "SETUP_DISPATCH_SIGNATURE_MISMATCH", "SETUP_FAULT_BOUNDARY_NOT_EMPTY",
            "SETUP_INTEGER_OUT_OF_RANGE", "SETUP_INVOCATION_FAILED", "SETUP_METHOD_NOT_AUTHORIZED",
            "SETUP_NULL_RESULT", "SETUP_NULL_RESULT_PROPERTY", "SETUP_PARTICIPANT_MATERIALIZATION_FAILED",
            "SETUP_PENDING_EVENT_BASELINE_NOT_EMPTY", "SETUP_REPLAY_CONTROL_FAILED",
            "SETUP_RESULT_EVIDENCE_FAILED", "SETUP_RESULT_TYPE_MISMATCH", "SETUP_VALIDATION_FAILED",
            "SETUP_VALUE_TYPE_MISMATCH", "UNSUPPORTED_LOCAL_DATE_EXPRESSION", "UNSUPPORTED_SETUP_ASSIGNMENT",
            "UNSUPPORTED_SETUP_CONSTRUCTOR", "UNSUPPORTED_SETUP_GENERIC_TYPE", "UNSUPPORTED_SETUP_LITERAL",
            "UNSUPPORTED_SETUP_RESULT_PROPERTY");
    private static final Set<String> PRE_ACTION_SOURCE_FAILURE_REASONS = Set.of(
            "MIXED_PREREQUISITE_AND_SETUP", "DUPLICATE_SETUP_METHOD_DISPATCH",
            "INVALID_SETUP_RESULT_REFERENCE", "MALFORMED_SETUP_DISPATCHER", "MISSING_SETUP_RUNTIME_TYPE",
            "SETUP_DECLARED_TYPE_MISMATCH", "SETUP_DISPATCH_FAILED", "SETUP_DISPATCH_SIGNATURE_MISMATCH",
            "SETUP_FAULT_BOUNDARY_NOT_EMPTY", "SETUP_METHOD_NOT_AUTHORIZED", "SETUP_REPLAY_CONTROL_FAILED",
            "SETUP_VALIDATION_FAILED", "UNSUPPORTED_LOCAL_DATE_EXPRESSION", "UNSUPPORTED_SETUP_ASSIGNMENT",
            "UNSUPPORTED_SETUP_CONSTRUCTOR", "UNSUPPORTED_SETUP_GENERIC_TYPE", "UNSUPPORTED_SETUP_LITERAL",
            "UNSUPPORTED_SETUP_RESULT_PROPERTY");
    private static final Set<String> MATERIALIZATION_FAILURE_REASONS = Set.of(
            "BASELINE_BINDING_TYPE_MISMATCH", "MATERIALIZATION_EXCEPTION", "MATERIALIZATION_FAILED",
            "MISSING_BASELINE_BINDING", "MISSING_INPUT_RECIPE", "MISSING_RECIPE", "MISSING_TARGET_TYPE",
            "UNMATERIALIZABLE_ASSIGNMENT", "UNMATERIALIZABLE_RECEIVER", "UNRESOLVED_ARGUMENT",
            "UNRESOLVED_CONSTRUCTOR_ARGUMENT", "UNRESOLVED_PLACEHOLDER", "UNRESOLVED_VALUE",
            "UNSUPPORTED_CALL_RESULT", "UNSUPPORTED_RECIPE_KIND", "UNSUPPORTED_TRANSFORM",
            "UNSUPPORTED_TRANSFORM_RECEIVER");

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
                    if (!Files.isRegularFile(workerOutput)) {
                        throw new IllegalStateException("isolated worker exited with status " + status
                                + " without a report");
                    }
                    ScenarioSetupPreflightReport worker = readWorkerReport(workerOutput);
                    validateWorkerReport(worker, attempt, plan, expectedManifestPath, options, status);
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
            require(workload.path("workloadPlanId").isTextual(),
                    "worker workload id is missing or malformed");
            require(workload.path("status").isTextual(),
                    "worker workload status is missing or malformed");
            require(workload.path("setupDurationNanos").isIntegralNumber()
                            && workload.path("setupDurationNanos").canConvertToLong(),
                    "worker workload duration is missing or malformed");
            require(workload.path("participants").isArray(),
                    "worker workload participants are missing or malformed");
            require(workload.path("blockers").isArray(),
                    "worker workload blockers are missing or malformed");
            JsonNode sourceSetup = workload.path("sourceSetup");
            if (sourceSetup.isObject()) {
                require(sourceSetup.path("status").isTextual(),
                        "worker source setup status is missing or malformed");
                require(sourceSetup.path("durationNanos").isIntegralNumber()
                                && sourceSetup.path("durationNanos").canConvertToLong(),
                        "worker source setup duration is missing or malformed");
                require(sourceSetup.path("pendingEventsCleared").isIntegralNumber()
                                && sourceSetup.path("pendingEventsCleared").canConvertToLong(),
                        "worker source setup pending-event count is missing or malformed");
                require(sourceSetup.path("emptyPendingEventBaseline").isBoolean(),
                        "worker source setup event baseline is missing or malformed");
                require(sourceSetup.path("actions").isArray(),
                        "worker source setup actions are missing or malformed");
                require(sourceSetup.path("participantBindings").isArray(),
                        "worker report source setup participant bindings are missing or malformed");
                require(sourceSetup.path("failureReason").isNull() || sourceSetup.path("failureReason").isTextual(),
                        "worker source setup failure reason is malformed");
                require(sourceSetup.path("failureMessage").isNull() || sourceSetup.path("failureMessage").isTextual(),
                        "worker source setup failure message is malformed");
            } else {
                require(sourceSetup.isNull() || sourceSetup.isMissingNode(),
                        "worker source setup is malformed");
            }
            for (JsonNode participant : workload.path("participants")) {
                require(participant.isObject()
                                && participant.path("sagaInstanceId").isTextual()
                                && participant.path("sagaFqn").isTextual()
                                && participant.path("inputVariantId").isTextual()
                                && participant.path("setupReady").isBoolean()
                                && participant.path("materializationState").isTextual()
                                && participant.path("startupState").isTextual()
                                && participant.path("blockers").isArray(),
                        "worker report contains a malformed participant result");
            }
        }
        return mapper.treeToValue(envelope, ScenarioSetupPreflightReport.class);
    }

    private void validateWorkerReport(ScenarioSetupPreflightReport worker,
                                      Set<String> expectedWorkloads,
                                      ScenarioExecutor.PreflightPlan plan,
                                      Path expectedManifestPath,
                                      ScenarioSetupPreflightOptions options,
                                      int workerExitStatus) {
        require(ScenarioSetupPreflightReport.SCHEMA_VERSION.equals(worker.schemaVersion()),
                "unexpected worker report schema " + worker.schemaVersion());
        require(workerExitStatus == 0 || workerExitStatus == 1,
                "unexpected worker exit status " + workerExitStatus);
        require("SUCCESS".equals(worker.terminalStatus()) || "SETUP_FAILED".equals(worker.terminalStatus()),
                "unexpected worker terminal status " + worker.terminalStatus());
        require((workerExitStatus == 0) == worker.successful(),
                "worker exit status " + workerExitStatus + " is inconsistent with terminal status "
                        + worker.terminalStatus());
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
        boolean allReady = true;
        for (ScenarioSetupPreflightReport.WorkloadResult workload : worker.workloads()) {
            require(workload.setupDurationNanos() >= 0,
                    "worker workload reported a negative duration: " + workload.workloadPlanId());
            require(!workload.participants().isEmpty(),
                    "worker workload has no participants: " + workload.workloadPlanId());
            ScenarioExecutor.ExpectedWorkload expected = plan.expectedWorkloads().get(workload.workloadPlanId());
            if (!plan.expectedWorkloads().isEmpty()) {
                require(expected != null, "worker workload has no expected package identity: "
                        + workload.workloadPlanId());
                validateWorkloadIdentity(workload, expected);
            }
            boolean sourceSetupExpected = plan.sourceSetupWorkloadIds().contains(workload.workloadPlanId());
            if ("SETUP_READY".equals(workload.status())) {
                validateReadyWorkload(workload, sourceSetupExpected, expected);
            } else {
                allReady = false;
                validateFailedWorkload(workload, sourceSetupExpected, expected);
            }
        }
        require(worker.successful() == allReady,
                "worker terminal status is inconsistent with workload results");
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

    private void validateReadyWorkload(ScenarioSetupPreflightReport.WorkloadResult workload,
                                       boolean sourceSetupExpected,
                                       ScenarioExecutor.ExpectedWorkload expected) {
        require(workload.blockers().isEmpty(),
                "worker workload contains blockers: " + workload.workloadPlanId());
        for (ScenarioSetupPreflightReport.ParticipantResult participant : workload.participants()) {
            require(participant.setupReady()
                            && "MATERIALIZED".equals(participant.materializationState())
                            && "STARTUP_READY".equals(participant.startupState())
                            && participant.blockers().isEmpty(),
                    "worker participant result is inconsistent with SETUP_READY: "
                            + workload.workloadPlanId());
        }
        require(sourceSetupExpected == (workload.sourceSetup() != null),
                "worker source-setup evidence does not match workload selection: "
                        + workload.workloadPlanId());
        if (sourceSetupExpected) validateSuccessfulSourceSetup(workload, expected, true);
    }

    private void validateFailedWorkload(ScenarioSetupPreflightReport.WorkloadResult workload,
                                        boolean sourceSetupExpected,
                                        ScenarioExecutor.ExpectedWorkload expected) {
        require(workload.status() != null && !workload.status().isBlank(),
                "worker failed workload has no status: " + workload.workloadPlanId());
        require(!workload.blockers().isEmpty(),
                "worker failed workload has no blockers: " + workload.workloadPlanId());

        ScenarioExecutionReport.SourceSetup sourceSetup = workload.sourceSetup();
        if (!sourceSetupExpected) {
            require(sourceSetup == null,
                    "worker source-setup evidence does not match workload selection: "
                            + workload.workloadPlanId());
        } else if ("PREREQUISITE_BASELINE_FAILED".equals(workload.status())) {
            require(sourceSetup == null,
                    "worker reported source setup after prerequisite failure: " + workload.workloadPlanId());
        } else {
            require(sourceSetup != null,
                    "worker source-setup evidence is missing: " + workload.workloadPlanId());
        }

        if ("MATERIALIZATION_FAILED".equals(workload.status())) {
            if (sourceSetupExpected) validateSuccessfulSourceSetup(workload, expected, true);
            require(workload.participants().stream().allMatch(participant ->
                            !participant.setupReady()
                                    && ("MATERIALIZED".equals(participant.materializationState())
                                    || "MATERIALIZATION_FAILED".equals(participant.materializationState()))
                                    && "NOT_ATTEMPTED".equals(participant.startupState())
                                    && !participant.blockers().isEmpty()),
                    "worker materialization failure has inconsistent participant evidence: "
                            + workload.workloadPlanId());
            require(workload.participants().stream().anyMatch(participant ->
                            "MATERIALIZATION_FAILED".equals(participant.materializationState())
                    ),
                    "worker materialization failure has inconsistent participant evidence: "
                            + workload.workloadPlanId());
            require(workload.blockers().stream().allMatch(blocker ->
                            MATERIALIZATION_FAILURE_REASONS.contains(blocker.reason())
                                    && workload.participants().stream().anyMatch(participant ->
                                    participant.blockers().contains(blocker))),
                    "worker materialization blockers are inconsistent with participants");
            require(workload.participants().stream()
                            .filter(participant -> "MATERIALIZED".equals(participant.materializationState()))
                            .allMatch(participant -> participant.blockers().size() == 1
                                    && "SETUP_NOT_COMPLETED".equals(participant.blockers().get(0).reason())),
                    "worker materialized participant has inconsistent stop evidence");
        } else if ("STARTUP_FAILED".equals(workload.status())) {
            if (sourceSetupExpected) validateSuccessfulSourceSetup(workload, expected, true);
            require(workload.participants().stream().allMatch(participant ->
                            "MATERIALIZED".equals(participant.materializationState())
                                    && ("STARTUP_READY".equals(participant.startupState())
                                    ? participant.setupReady() && participant.blockers().isEmpty()
                                    : "STARTUP_FAILED".equals(participant.startupState())
                                    && !participant.setupReady() && !participant.blockers().isEmpty())),
                    "worker startup failure has inconsistent participant evidence: " + workload.workloadPlanId());
            require(workload.participants().stream().anyMatch(participant ->
                            "STARTUP_FAILED".equals(participant.startupState())),
                    "worker startup failure has no failed participant: " + workload.workloadPlanId());
            require(workload.blockers().stream().allMatch(blocker ->
                            "STARTUP_FAILED".equals(blocker.reason())
                                    && workload.participants().stream().anyMatch(participant ->
                                    participant.blockers().contains(blocker))),
                    "worker startup blockers are inconsistent with participants");
        } else if ("PREREQUISITE_BASELINE_FAILED".equals(workload.status())) {
            require(workload.participants().stream().allMatch(participant ->
                            !participant.setupReady()
                                    && "NOT_ATTEMPTED".equals(participant.materializationState())
                                    && "NOT_ATTEMPTED".equals(participant.startupState())),
                    "worker prerequisite failure has inconsistent participant evidence: "
                            + workload.workloadPlanId());
            require(workload.blockers().stream().allMatch(blocker ->
                            "PREREQUISITE_BASELINE_FAILED".equals(blocker.reason()))
                            && workload.participants().stream().allMatch(participant ->
                            participant.blockers().size() == 1
                                    && "SETUP_NOT_COMPLETED".equals(participant.blockers().get(0).reason())),
                    "worker prerequisite blockers are inconsistent");
        } else {
            require(SOURCE_SETUP_FAILURE_REASONS.contains(workload.status())
                            && sourceSetupExpected && sourceSetup != null
                            && "FAILED".equals(sourceSetup.status())
                            && workload.status().equals(sourceSetup.failureReason())
                            && sourceSetup.failureMessage() != null
                            && !sourceSetup.failureMessage().isBlank()
                            && sourceSetup.pendingEventsCleared() >= 0
                            && !sourceSetup.emptyPendingEventBaseline()
                            && sourceSetup.actions().stream().allMatch(action ->
                            "SUCCEEDED".equals(action.status()) || "FAILED".equals(action.status()))
                            && sourceSetup.participantBindings().stream().allMatch(binding ->
                            "RESOLVED".equals(binding.status()))
                            && workload.blockers().stream().anyMatch(blocker ->
                            workload.status().equals(blocker.reason())),
                    "worker source setup failure evidence is inconsistent: "
                            + workload.workloadPlanId());
            require(workload.blockers().size() == 1
                            && workload.status().equals(workload.blockers().get(0).reason())
                            && Objects.equals(sourceSetup.failureMessage(), workload.blockers().get(0).message()),
                    "worker source setup blocker is inconsistent with failure evidence");
            validateSourceSetupIdentities(workload, expected, false);
            require(workload.participants().stream().allMatch(participant ->
                            participant.blockers().size() == 1
                                    && "SETUP_NOT_COMPLETED".equals(participant.blockers().get(0).reason())),
                    "worker source setup participant blockers are inconsistent");
            require(workload.participants().stream().allMatch(participant ->
                            !participant.setupReady()
                                    && "NOT_ATTEMPTED".equals(participant.materializationState())
                                    && "NOT_ATTEMPTED".equals(participant.startupState())
                                    && !participant.blockers().isEmpty()),
                    "worker source setup failure has inconsistent participant evidence: "
                            + workload.workloadPlanId());
        }
    }

    private void validateSuccessfulSourceSetup(ScenarioSetupPreflightReport.WorkloadResult workload,
                                               ScenarioExecutor.ExpectedWorkload expected,
                                               boolean complete) {
        ScenarioExecutionReport.SourceSetup sourceSetup = workload.sourceSetup();
        require(sourceSetup != null
                        && "SUCCEEDED".equals(sourceSetup.status())
                        && sourceSetup.pendingEventsCleared() >= 0
                        && sourceSetup.emptyPendingEventBaseline()
                        && sourceSetup.failureReason() == null
                        && sourceSetup.failureMessage() == null
                        && !sourceSetup.actions().isEmpty()
                        && sourceSetup.actions().stream()
                        .allMatch(action -> "SUCCEEDED".equals(action.status()))
                        && sourceSetup.participantBindings() != null
                        && sourceSetup.participantBindings().stream()
                        .allMatch(binding -> "RESOLVED".equals(binding.status())),
                "worker source setup did not complete successfully: " + workload.workloadPlanId());
        validateSourceSetupIdentities(workload, expected, complete);
    }

    private void validateWorkloadIdentity(ScenarioSetupPreflightReport.WorkloadResult workload,
                                          ScenarioExecutor.ExpectedWorkload expected) {
        List<ScenarioExecutor.ExpectedParticipant> actual = workload.participants().stream()
                .map(participant -> new ScenarioExecutor.ExpectedParticipant(
                        participant.sagaInstanceId(), participant.sagaFqn(), participant.inputVariantId()))
                .toList();
        require(actual.equals(expected.participants()),
                "worker participant identities do not match the selected workload: "
                        + workload.workloadPlanId());
        workload.blockers().forEach(blocker -> require(
                workload.workloadPlanId().equals(blocker.workloadPlanId()),
                "worker workload blocker belongs to a different workload"));
        for (ScenarioSetupPreflightReport.ParticipantResult participant : workload.participants()) {
            participant.blockers().forEach(blocker -> require(
                    workload.workloadPlanId().equals(blocker.workloadPlanId())
                            && (blocker.inputVariantId() == null
                            || participant.inputVariantId().equals(blocker.inputVariantId())),
                    "worker participant blocker identity is inconsistent"));
        }
    }

    private void validateSourceSetupIdentities(ScenarioSetupPreflightReport.WorkloadResult workload,
                                               ScenarioExecutor.ExpectedWorkload expected,
                                               boolean complete) {
        if (expected == null) return;
        ScenarioExecutionReport.SourceSetup setup = workload.sourceSetup();
        List<ScenarioExecutionReport.SetupActionOutcome> actions = setup.actions();
        require(actions.size() <= expected.setupActions().size(),
                "worker source setup action identities do not match the selected workload");
        for (int index = 0; index < actions.size(); index++) {
            ScenarioExecutionReport.SetupActionOutcome actual = actions.get(index);
            ScenarioExecutor.ExpectedSetupAction expectedAction = expected.setupActions().get(index);
            require(Objects.equals(actual.actionId(), expectedAction.actionId())
                            && actual.orderIndex() == expectedAction.orderIndex()
                            && Objects.equals(actual.methodKey(), expectedAction.methodKey())
                            && (expectedAction.declaredResultTypeFqn() == null
                            || Objects.equals(actual.declaredResultTypeFqn(), expectedAction.declaredResultTypeFqn())),
                    "worker source setup action identities do not match the selected workload");
        }
        List<ScenarioExecutor.ExpectedSetupBinding> bindings = setup.participantBindings().stream()
                .map(binding -> new ScenarioExecutor.ExpectedSetupBinding(
                        binding.inputVariantId(), binding.argumentIndex(), binding.sourceActionId(),
                        binding.propertyName()))
                .toList();
        require(bindings.size() <= expected.setupBindings().size()
                        && bindings.equals(expected.setupBindings().subList(0, bindings.size())),
                "worker source setup binding identities do not match the selected workload");
        if (complete) {
            require(actions.size() == expected.setupActions().size()
                            && actions.stream().allMatch(action -> "SUCCEEDED".equals(action.status()))
                            && bindings.equals(expected.setupBindings()),
                    "worker successful source setup evidence is incomplete");
            return;
        }

        String reason = setup.failureReason();
        if (PRE_ACTION_SOURCE_FAILURE_REASONS.contains(reason)) {
            require(actions.isEmpty() && bindings.isEmpty(),
                    "worker pre-action source failure contains impossible progress evidence");
            return;
        }
        long failedActions = actions.stream().filter(action -> "FAILED".equals(action.status())).count();
        if (failedActions > 0) {
            require(failedActions == 1
                            && "FAILED".equals(actions.get(actions.size() - 1).status())
                            && actions.subList(0, actions.size() - 1).stream()
                            .allMatch(action -> "SUCCEEDED".equals(action.status()))
                            && bindings.isEmpty(),
                    "worker source action failure progress is inconsistent");
        } else {
            require(actions.size() == expected.setupActions().size()
                            && actions.stream().allMatch(action -> "SUCCEEDED".equals(action.status())),
                    "worker post-action source failure has incomplete action evidence");
            if ("SETUP_CLEANUP_FAILED".equals(reason)
                    || "SETUP_PENDING_EVENT_BASELINE_NOT_EMPTY".equals(reason)) {
                require(bindings.isEmpty(),
                        "worker setup cleanup failure contains binding evidence");
            }
        }
    }

    private void require(boolean condition, String message) {
        if (!condition) throw new IllegalStateException(message);
    }

    private List<Set<String>> attempts(ScenarioExecutor.PreflightPlan plan) {
        List<Set<String>> attempts = new ArrayList<>();
        Set<String> providerBacked = new LinkedHashSet<>(plan.candidateWorkloadIds());
        providerBacked.removeAll(plan.sourceSetupWorkloadIds());
        if (!providerBacked.isEmpty()) attempts.add(Set.copyOf(providerBacked));
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
