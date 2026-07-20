package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;

import java.util.List;

record CatalogScenarioRecord(LegacyScenarioPlan plan,
                             DynamicEvidenceJoinStatus joinStatus,
                             int lineNumber,
                             String catalogKind,
                             String catalogPath) {
}

/** Temporary executor-only v2 compile bridge. Removed when persisted v3 replay lands. */
record LegacyScenarioPlan(
        String schemaVersion,
        String deterministicId,
        ScenarioKind kind,
        List<SagaInstance> sagaInstances,
        List<InputVariant> inputs,
        List<ScheduledStep> expandedSchedule,
        LegacyFaultSpace faultSpace,
        List<ConflictEvidence> conflictEvidence,
        List<String> warnings) {

    static final String SCHEMA_VERSION = "microservices-simulator.scenario-catalog.v2";

    LegacyScenarioPlan {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        kind = kind == null ? ScenarioKind.SINGLE_SAGA : kind;
        sagaInstances = sagaInstances == null ? List.of() : List.copyOf(sagaInstances);
        inputs = inputs == null ? List.of() : List.copyOf(inputs);
        expandedSchedule = expandedSchedule == null ? List.of() : List.copyOf(expandedSchedule);
        faultSpace = faultSpace == null ? LegacyFaultSpace.fromScheduledSteps(expandedSchedule) : faultSpace;
        conflictEvidence = conflictEvidence == null ? List.of() : List.copyOf(conflictEvidence);
        warnings = warnings == null ? List.of() : List.copyOf(warnings);
    }
}

record LegacyFaultSpace(int length,
                        List<String> scheduledStepIds,
                        String defaultVector) {
    LegacyFaultSpace {
        length = Math.max(0, length);
        scheduledStepIds = scheduledStepIds == null ? List.of() : List.copyOf(scheduledStepIds);
        defaultVector = defaultVector == null || defaultVector.isBlank() ? "0".repeat(length) : defaultVector;
    }

    static LegacyFaultSpace fromScheduledSteps(List<ScheduledStep> scheduledSteps) {
        List<String> ids = scheduledSteps == null ? List.of() : scheduledSteps.stream()
                .map(ScheduledStep::deterministicId)
                .toList();
        return new LegacyFaultSpace(ids.size(), ids, null);
    }
}
