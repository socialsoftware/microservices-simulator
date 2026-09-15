package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.annotation.JsonInclude;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Independent metadata-only diagnostic. Contains neither application payloads nor a score. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SagaReadExposureReport(
        String schemaVersion,
        String executionAttemptId,
        String workloadPlanId,
        String faultScenarioId,
        String executionTerminalStatus,
        String scheduleConformance,
        String executionValidity,
        String collectionCoverage,
        String collectionReason,
        @JsonInclude(JsonInclude.Include.ALWAYS) Integer observedExposureCount,
        String scope,
        List<String> excludedPaths,
        List<ReadResponseEvidence.Contract> contracts,
        List<ArtifactReference> artifacts,
        boolean baselineAbsenceCovered,
        List<Revision> baseline,
        SourceContract sourceContract,
        List<Action> actions,
        List<Write> committedWrites,
        List<Call> calls,
        List<Assessment> assessments,
        List<Finding> findings,
        List<Gap> gaps) {
    public static final String SCHEMA_VERSION = "microservices-simulator.saga-read-exposure.v2";
    public static final String SCOPE = "Exact declared outer-response contracts in measured synchronous Saga/local calls";
    public static final List<String> EXCLUDED_PATHS = List.of("SETUP", "OBSERVERS_AND_PROBES", "RECOVERY_READS",
            "INTERNAL_READS", "LISTS_AND_PREDICATES", "NESTED_REFERENCES", "IN_MEMORY_REUSE", "EVENT_CONSUMERS",
            "UNMAPPED_COMMANDS", "GRPC_STREAM_TCC", "DIRECT_WRITES_OUTSIDE_FRAMEWORK");

    public SagaReadExposureReport {
        schemaVersion = SCHEMA_VERSION;
        excludedPaths = copy(excludedPaths); contracts = copy(contracts); artifacts = copy(artifacts);
        baseline = copy(baseline); actions = copy(actions); committedWrites = copy(committedWrites);
        calls = copy(calls); assessments = copy(assessments); findings = copy(findings); gaps = copy(gaps);
    }

    public record Revision(ImpactEvidence.AggregateIdentity identity, String runtimeType, Long version,
                           String lifecycleState, boolean frameworkMetadataAvailable,
                           ImpactEvidence.AggregateIdentity predecessorIdentity, Long predecessorVersion,
                           Map<String, String> applicationAttributeFingerprints) {
        public Revision {
            applicationAttributeFingerprints = applicationAttributeFingerprints == null ? Map.of()
                    : java.util.Collections.unmodifiableMap(new TreeMap<>(applicationAttributeFingerprints));
        }
        static Revision from(ImpactEvidence.AggregateSnapshot value) {
            if (value == null) return null;
            var metadata = value.frameworkMetadata();
            return new Revision(value.identity(), value.runtimeType(), value.version(), value.lifecycleState(),
                    metadata != null, metadata == null ? null : metadata.predecessorIdentity(),
                    metadata == null ? null : metadata.predecessorVersion(),
                    SagaReadAttributeFingerprinter.fingerprint(value.applicationData()));
        }
    }

    public record Write(String id, long order, Revision revision, ImpactEvidence.Writer writer) { }
    public record Call(String id, long order, ReadResponseEvidence.Observation observation) { }
    public record Gap(long afterOrder, String stage, String subject, String reason) { }
    public record Assessment(String callId, String verdict, String reason, String findingId) { }
    public record Finding(String id, String verdict, String category,
                          ImpactEvidence.AggregateIdentity identity, String runtimeType,
                          Long producedVersion, Long recoveryVersion,
                          String producerSagaId, String readerSagaId,
                          String forwardWriteId, String recoveryWriteId,
                          String sourceScheduledStepId, String checkpointId,
                          List<String> restoredAttributes, List<String> notRestoredAttributes,
                          Long createdVersion, Long deletedVersion,
                          String creationWriteId, String deletionWriteId,
                          List<String> deliveryIds) {
        public Finding {
            restoredAttributes = copy(restoredAttributes);
            notRestoredAttributes = copy(notRestoredAttributes);
            deliveryIds = copy(deliveryIds);
        }
    }

    /** The minimal source facts needed to audit an occurrence/checkpoint join. */
    public record SourceContract(List<Occurrence> occurrences, List<Checkpoint> checkpoints) {
        public SourceContract { occurrences = copy(occurrences); checkpoints = copy(checkpoints); }
        public static SourceContract from(WorkloadPlan workload) {
            if (workload == null) return new SourceContract(List.of(), List.of());
            return new SourceContract(workload.forwardSchedule().stream().map(step -> new Occurrence(
                    step.deterministicId(), step.sagaInstanceId(), step.stepId(), step.runtimeStepName(),
                    step.scheduleOrder())).toList(), workload.compensationCheckpoints().stream().map(checkpoint ->
                    new Checkpoint(checkpoint.deterministicId(), checkpoint.sagaInstanceId(),
                            checkpoint.sourceScheduledStepId(), checkpoint.stepId(), checkpoint.runtimeStepName(),
                            checkpoint.occurrenceId(), checkpoint.evidenceClass() == null ? null
                            : checkpoint.evidenceClass().name())).toList());
        }
    }
    public record Occurrence(String id, String sagaInstanceId, String stepId, String runtimeStepName, int order) { }
    public record Checkpoint(String id, String sagaInstanceId, String sourceScheduledStepId, String stepId,
                             String runtimeStepName, String occurrenceId, String evidenceClass) { }

    /** Exception messages and setup values are deliberately absent. */
    public record Action(String id, String kind, String sagaInstanceId, String functionalityName,
                         String sourceScheduledStepId, String sourceStepId, String runtimeStepName,
                         String checkpointId, String compensationEvidenceClass, String runtimeOccurrenceId,
                         Integer plannedPosition, int actualPosition, String status, String bodyOutcome,
                         String commitOutcome, List<Recovery> recovery) {
        public Action { recovery = copy(recovery); }
        static Action from(ScenarioExecutionReport.ActionOutcome value, ScenarioExecutionReport execution) {
            List<String> names = execution.participants().stream()
                    .filter(participant -> java.util.Objects.equals(participant.sagaInstanceId(), value.sagaInstanceId()))
                    .map(ScenarioExecutionReport.Participant::sagaFqn).distinct().toList();
            return new Action(value.actionId(), value.kind(), value.sagaInstanceId(), names.size() == 1 ? names.getFirst() : null,
                    value.sourceScheduledStepId(), value.sourceStepId(), value.runtimeStepName(),
                    value.sourceCompensationCheckpointId(), value.compensationEvidenceClass(), value.runtimeOccurrenceId(),
                    value.plannedPosition(), value.actualPosition(), value.status(), value.bodyOutcome(), value.commitOutcome(),
                    value.recoverySubOutcomes().stream().map(result -> new Recovery(result.kind(), result.status())).toList());
        }
    }
    public record Recovery(String kind, String status) { }

    public record ArtifactReference(String role, String path, String sha256, String status, String reason) {
        public static ArtifactReference file(String role, Path path) {
            if (path == null) return new ArtifactReference(role, null, null, "UNAVAILABLE", "PATH_NOT_CONFIGURED");
            try (var input = Files.newInputStream(path)) {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] buffer = new byte[8192];
                int count;
                while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
                return new ArtifactReference(role, path.toAbsolutePath().normalize().toString(),
                        HexFormat.of().formatHex(digest.digest()), "AVAILABLE", null);
            } catch (IOException | NoSuchAlgorithmException failure) {
                return new ArtifactReference(role, path.toAbsolutePath().normalize().toString(), null,
                        "UNAVAILABLE", "ARTIFACT_HASH_UNAVAILABLE");
            }
        }
    }

    static <T> List<T> copy(List<T> values) { return values == null ? List.of() : List.copyOf(values); }
}
