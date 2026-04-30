package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record ScenarioPlan(
        String schemaVersion,
        String deterministicId,
        ScenarioKind kind,
        List<SagaInstance> sagaInstances,
        List<InputVariant> inputs,
        List<ScheduledStep> expandedSchedule,
        FaultSpace faultSpace,
        List<ConflictEvidence> conflictEvidence,
        List<String> warnings) {

    public static final String SCHEMA_VERSION = "microservices-simulator.scenario-catalog.v1";

    public ScenarioPlan {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        deterministicId = normalize(deterministicId);
        kind = kind == null ? ScenarioKind.SINGLE_SAGA : kind;
        sagaInstances = sagaInstances == null ? List.of() : List.copyOf(sagaInstances);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        expandedSchedule = expandedSchedule == null ? List.of() : List.copyOf(expandedSchedule);
        faultSpace = faultSpace == null ? FaultSpace.fromScheduledSteps(expandedSchedule) : faultSpace;
        conflictEvidence = conflictEvidence == null ? List.of() : List.copyOf(conflictEvidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
