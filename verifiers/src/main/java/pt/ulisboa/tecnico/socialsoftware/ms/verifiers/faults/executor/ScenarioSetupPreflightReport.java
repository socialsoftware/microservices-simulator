package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ScenarioSetupPreflightReport(
        String schemaVersion,
        String preflightAttemptId,
        String terminalStatus,
        String packageManifestPath,
        String candidateSelection,
        int candidateCount,
        int participantCount,
        long setupDurationNanos,
        ScenarioExecutionReport.RuntimeMetadata runtimeMetadata,
        List<WorkloadResult> workloads) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-setup-preflight-report.v1";
    public static final String CANDIDATE_SELECTION = "MANIFEST_DECLARED_MATERIALIZABLE";

    public ScenarioSetupPreflightReport {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        candidateSelection = candidateSelection == null || candidateSelection.isBlank()
                ? CANDIDATE_SELECTION
                : candidateSelection;
        workloads = workloads == null ? List.of() : List.copyOf(workloads);
    }

    public boolean successful() {
        return "SUCCESS".equals(terminalStatus);
    }

    public record WorkloadResult(
            String workloadPlanId,
            String status,
            long setupDurationNanos,
            ScenarioExecutionReport.SourceSetup sourceSetup,
            List<ParticipantResult> participants,
            List<ScenarioExecutionReport.Blocker> blockers) {
        public WorkloadResult {
            participants = participants == null ? List.of() : List.copyOf(participants);
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
        }
    }

    public record ParticipantResult(
            String sagaInstanceId,
            String sagaFqn,
            String inputVariantId,
            boolean setupReady,
            String materializationState,
            String startupState,
            List<ScenarioExecutionReport.Blocker> blockers) {
        public ParticipantResult {
            blockers = blockers == null ? List.of() : List.copyOf(blockers);
        }
    }
}
