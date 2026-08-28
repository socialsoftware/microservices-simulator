package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;
import java.util.stream.IntStream;

public record WorkloadPlan(
        String schemaVersion,
        String deterministicId,
        ScenarioKind kind,
        WorkloadExecutionShape executionShape,
        List<SagaInstance> participants,
        List<InputVariant> acceptedInputs,
        List<ScheduledStep> forwardSchedule,
        List<EventConsequence> eventConsequences,
        List<NormalActionRef> normalSchedule,
        PrerequisiteBaseline prerequisiteBaseline,
        SetupPlan setupPlan,
        List<ConflictEvidence> conflictEvidence,
        List<ForwardFaultSlot> faultSlots,
        List<CompensationCheckpoint> compensationCheckpoints,
        List<String> warnings) {

    public static final String SCHEMA_VERSION = "microservices-simulator.workload-plan.v5";
    public static final String LEGACY_V4_SCHEMA_VERSION = "microservices-simulator.workload-plan.v4";

    public WorkloadPlan {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        deterministicId = normalize(deterministicId);
        kind = kind == null ? ScenarioKind.SINGLE_SAGA : kind;
        executionShape = executionShape == null ? WorkloadExecutionShape.SAGA_LOCAL : executionShape;
        participants = participants == null ? List.of() : List.copyOf(participants);
        acceptedInputs = acceptedInputs == null ? List.of() : List.copyOf(acceptedInputs);
        forwardSchedule = forwardSchedule == null ? List.of() : List.copyOf(forwardSchedule);
        eventConsequences = eventConsequences == null ? List.of() : List.copyOf(eventConsequences);
        normalSchedule = normalSchedule == null ? List.of() : List.copyOf(normalSchedule);
        conflictEvidence = conflictEvidence == null ? List.of() : List.copyOf(conflictEvidence);
        faultSlots = faultSlots == null ? List.of() : List.copyOf(faultSlots);
        compensationCheckpoints = compensationCheckpoints == null ? List.of() : List.copyOf(compensationCheckpoints);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    public WorkloadPlan(String schemaVersion,
                        String deterministicId,
                        ScenarioKind kind,
                        WorkloadExecutionShape executionShape,
                        List<SagaInstance> participants,
                        List<InputVariant> acceptedInputs,
                        List<ScheduledStep> forwardSchedule,
                        List<EventConsequence> eventConsequences,
                        List<NormalActionRef> normalSchedule,
                        List<ConflictEvidence> conflictEvidence,
                        List<ForwardFaultSlot> faultSlots,
                        List<CompensationCheckpoint> compensationCheckpoints,
                        List<String> warnings) {
        this(schemaVersion, deterministicId, kind, executionShape, participants, acceptedInputs,
                forwardSchedule, eventConsequences, normalSchedule, null, null, conflictEvidence,
                faultSlots, compensationCheckpoints, warnings);
    }

    public WorkloadPlan(String schemaVersion,
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
        this(schemaVersion, deterministicId, kind, executionShape, participants, acceptedInputs,
                forwardSchedule, List.of(), forwardsOnly(forwardSchedule), null, null, conflictEvidence,
                faultSlots, compensationCheckpoints, warnings);
    }

    public WorkloadPlan(String schemaVersion,
                        String deterministicId,
                        ScenarioKind kind,
                        WorkloadExecutionShape executionShape,
                        List<SagaInstance> participants,
                        List<InputVariant> acceptedInputs,
                        List<ScheduledStep> forwardSchedule,
                        List<EventConsequence> eventConsequences,
                        List<NormalActionRef> normalSchedule,
                        PrerequisiteBaseline prerequisiteBaseline,
                        List<ConflictEvidence> conflictEvidence,
                        List<ForwardFaultSlot> faultSlots,
                        List<CompensationCheckpoint> compensationCheckpoints,
                        List<String> warnings) {
        this(schemaVersion, deterministicId, kind, executionShape, participants, acceptedInputs,
                forwardSchedule, eventConsequences, normalSchedule, prerequisiteBaseline, null,
                conflictEvidence, faultSlots, compensationCheckpoints, warnings);
    }

    private static List<NormalActionRef> forwardsOnly(List<ScheduledStep> schedule) {
        List<ScheduledStep> safe = schedule == null ? List.of() : schedule;
        return IntStream.range(0, safe.size())
                .mapToObj(index -> NormalActionRef.forward(index, safe.get(index).deterministicId()))
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
