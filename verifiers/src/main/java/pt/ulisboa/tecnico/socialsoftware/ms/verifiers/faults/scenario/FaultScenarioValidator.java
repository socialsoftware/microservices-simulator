package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class FaultScenarioValidator {

    public ValidationResult validate(FaultScenario scenario, WorkloadPlan workloadPlan) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (scenario == null) {
            return new ValidationResult(false, List.of(new Diagnostic("MISSING_FAULT_SCENARIO", "fault scenario is required")));
        }
        if (workloadPlan == null) {
            return new ValidationResult(false, List.of(new Diagnostic("MISSING_REFERENCED_WORKLOAD", scenario.workloadPlanId())));
        }
        if (!FaultScenario.SCHEMA_VERSION.equals(scenario.schemaVersion())) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_FAULT_SCENARIO_SCHEMA", scenario.schemaVersion()));
        }
        if (!Objects.equals(scenario.workloadPlanId(), workloadPlan.deterministicId())) {
            diagnostics.add(new Diagnostic("WORKLOAD_REFERENCE_MISMATCH", scenario.workloadPlanId()));
        }
        if (scenario.assignedVector() == null
                || scenario.assignedVector().length() != workloadPlan.faultSlots().size()
                || !scenario.assignedVector().matches("[01]*")) {
            diagnostics.add(new Diagnostic("INVALID_ASSIGNED_VECTOR", "vector must match the ordered workload fault slots"));
        }

        Map<String, ForwardFaultSlot> slotsById = new HashMap<>();
        workloadPlan.faultSlots().forEach(slot -> slotsById.put(slot.deterministicId(), slot));
        Map<String, CompensationCheckpoint> checkpointsById = new HashMap<>();
        workloadPlan.compensationCheckpoints().forEach(checkpoint -> checkpointsById.put(checkpoint.deterministicId(), checkpoint));
        Set<String> actionIds = new HashSet<>();
        for (FaultScenarioAction action : scenario.actions()) {
            validateAction(action, slotsById, checkpointsById, actionIds, diagnostics);
        }
        if (scenario.assignedVector() != null
                && !scenario.assignedVector().contains("1")
                && scenario.actions().stream().anyMatch(action -> action.kind() == FaultScenarioActionKind.COMPENSATION)) {
            diagnostics.add(new Diagnostic("ALL_ZERO_CONTAINS_COMPENSATION", scenario.deterministicId()));
        }

        String expectedId = ScenarioIdGenerator.faultScenarioId(scenario);
        if (scenario.deterministicId() == null) {
            diagnostics.add(new Diagnostic("MISSING_FAULT_SCENARIO_ID", "deterministicId is required"));
        } else if (!scenario.deterministicId().equals(expectedId)) {
            diagnostics.add(new Diagnostic("FAULT_SCENARIO_ID_MISMATCH", scenario.deterministicId()));
        }
        return new ValidationResult(diagnostics.isEmpty(), diagnostics);
    }

    private void validateAction(FaultScenarioAction action,
                                Map<String, ForwardFaultSlot> slotsById,
                                Map<String, CompensationCheckpoint> checkpointsById,
                                Set<String> actionIds,
                                List<Diagnostic> diagnostics) {
        if (action == null || action.deterministicId() == null || !actionIds.add(action.deterministicId())) {
            diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_ACTION_ID", action == null ? "null" : action.deterministicId()));
            return;
        }
        if (!action.deterministicId().equals(ScenarioIdGenerator.faultScenarioActionId(action))) {
            diagnostics.add(new Diagnostic("ACTION_ID_MISMATCH", action.deterministicId()));
        }
        if (action.kind() == FaultScenarioActionKind.FORWARD) {
            ForwardFaultSlot slot = slotsById.get(action.sourceFaultSlotId());
            if (slot == null
                    || action.sourceCompensationCheckpointId() != null
                    || !Objects.equals(action.sagaInstanceId(), slot.sagaInstanceId())
                    || !Objects.equals(action.occurrenceId(), slot.occurrenceId())) {
                diagnostics.add(new Diagnostic("MALFORMED_FORWARD_ACTION", action.deterministicId()));
            }
        } else if (action.kind() == FaultScenarioActionKind.COMPENSATION) {
            CompensationCheckpoint checkpoint = checkpointsById.get(action.sourceCompensationCheckpointId());
            if (checkpoint == null
                    || action.sourceFaultSlotId() != null
                    || !Objects.equals(action.sagaInstanceId(), checkpoint.sagaInstanceId())
                    || !Objects.equals(action.occurrenceId(), checkpoint.occurrenceId())) {
                diagnostics.add(new Diagnostic("MALFORMED_COMPENSATION_ACTION", action.deterministicId()));
            }
        } else {
            diagnostics.add(new Diagnostic("MISSING_ACTION_KIND", action.deterministicId()));
        }
    }

    public record ValidationResult(boolean valid, List<Diagnostic> diagnostics) {
        public ValidationResult {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    public record Diagnostic(String code, String message) {
    }
}
