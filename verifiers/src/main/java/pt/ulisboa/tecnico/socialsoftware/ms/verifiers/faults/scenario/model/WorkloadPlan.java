package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record WorkloadPlan(
        String schemaVersion,
        String deterministicId,
        ScenarioKind kind,
        WorkloadExecutionShape executionShape,
        List<SagaInstance> participants,
        List<InputVariant> acceptedInputs,
        List<ScheduledStep> forwardSchedule,
        List<ConflictEvidence> conflictEvidence,
        List<ForwardFaultSlot> faultSlots,
        List<CompensationCheckpoint> compensationCheckpoints,
        List<String> warnings) {

    public static final String SCHEMA_VERSION = "microservices-simulator.workload-plan.v3";

    public WorkloadPlan {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        deterministicId = normalize(deterministicId);
        kind = kind == null ? ScenarioKind.SINGLE_SAGA : kind;
        executionShape = executionShape == null ? WorkloadExecutionShape.SAGA_LOCAL : executionShape;
        participants = participants == null ? List.of() : List.copyOf(participants);
        acceptedInputs = acceptedInputs == null ? List.of() : List.copyOf(acceptedInputs);
        forwardSchedule = forwardSchedule == null ? List.of() : List.copyOf(forwardSchedule);
        conflictEvidence = conflictEvidence == null ? List.of() : List.copyOf(conflictEvidence);
        faultSlots = faultSlots == null ? List.of() : List.copyOf(faultSlots);
        compensationCheckpoints = compensationCheckpoints == null ? List.of() : List.copyOf(compensationCheckpoints);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
