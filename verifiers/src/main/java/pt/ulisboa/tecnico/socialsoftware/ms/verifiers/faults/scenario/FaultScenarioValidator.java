package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionRef;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.ArrayList;
import java.util.Collections;
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
        Map<String, EventConsequence> consequencesById = new HashMap<>();
        workloadPlan.eventConsequences().forEach(consequence -> consequencesById.put(consequence.deterministicId(), consequence));
        Set<String> actionIds = new HashSet<>();
        for (FaultScenarioAction action : scenario.actions()) {
            validateAction(action, slotsById, checkpointsById, consequencesById, actionIds, diagnostics);
        }
        if (scenario.assignedVector() != null
                && scenario.assignedVector().length() == workloadPlan.faultSlots().size()
                && scenario.assignedVector().matches("[01]*")) {
            validateActionSchedule(scenario, workloadPlan, slotsById, checkpointsById, diagnostics);
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
                                Map<String, EventConsequence> consequencesById,
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
                    || action.sourceEventConsequenceId() != null
                    || !Objects.equals(action.sagaInstanceId(), slot.sagaInstanceId())
                    || !Objects.equals(action.occurrenceId(), slot.occurrenceId())) {
                diagnostics.add(new Diagnostic("MALFORMED_FORWARD_ACTION", action.deterministicId()));
            }
        } else if (action.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE) {
            EventConsequence consequence = consequencesById.get(action.sourceEventConsequenceId());
            ForwardFaultSlot triggerSlot = consequence == null ? null : slotsById.values().stream()
                    .filter(slot -> Objects.equals(slot.scheduledStepId(), consequence.triggerScheduledStepId()))
                    .findFirst().orElse(null);
            if (consequence == null
                    || triggerSlot == null
                    || action.sourceFaultSlotId() != null
                    || action.sourceCompensationCheckpointId() != null
                    || !Objects.equals(action.sagaInstanceId(), triggerSlot.sagaInstanceId())
                    || !Objects.equals(action.occurrenceId(), consequence.deterministicId())) {
                diagnostics.add(new Diagnostic("MALFORMED_EVENT_CONSEQUENCE_ACTION", action.deterministicId()));
            }
        } else if (action.kind() == FaultScenarioActionKind.COMPENSATION) {
            CompensationCheckpoint checkpoint = checkpointsById.get(action.sourceCompensationCheckpointId());
            if (checkpoint == null
                    || action.sourceFaultSlotId() != null
                    || action.sourceEventConsequenceId() != null
                    || !Objects.equals(action.sagaInstanceId(), checkpoint.sagaInstanceId())
                    || !Objects.equals(action.occurrenceId(), checkpoint.occurrenceId())) {
                diagnostics.add(new Diagnostic("MALFORMED_COMPENSATION_ACTION", action.deterministicId()));
            }
        } else {
            diagnostics.add(new Diagnostic("MISSING_ACTION_KIND", action.deterministicId()));
        }
    }

    private void validateActionSchedule(FaultScenario scenario,
                                        WorkloadPlan workloadPlan,
                                        Map<String, ForwardFaultSlot> slotsById,
                                        Map<String, CompensationCheckpoint> checkpointsById,
                                        List<Diagnostic> diagnostics) {
        Map<String, ForwardFaultSlot> slotsByScheduledStep = new HashMap<>();
        workloadPlan.faultSlots().forEach(slot -> slotsByScheduledStep.put(slot.scheduledStepId(), slot));
        Map<String, EventConsequence> consequencesById = new HashMap<>();
        workloadPlan.eventConsequences().forEach(consequence -> consequencesById.put(consequence.deterministicId(), consequence));
        Map<String, CompensationCheckpoint> checkpointsBySourceStep = new HashMap<>();
        workloadPlan.compensationCheckpoints().forEach(checkpoint ->
                checkpointsBySourceStep.put(checkpoint.sourceScheduledStepId(), checkpoint));
        Map<String, List<CompensationCheckpoint>> completedCheckpoints = new HashMap<>();
        Map<String, List<CompensationCheckpoint>> enabledRecovery = new HashMap<>();
        Set<String> failedParticipants = new HashSet<>();
        Set<String> successfulScheduledSteps = new HashSet<>();
        int normalCursor = 0;

        for (FaultScenarioAction action : scenario.actions()) {
            if (action == null || action.kind() == null) {
                continue;
            }
            normalCursor = skipFailedOwnerForwards(
                    workloadPlan.normalSchedule(), normalCursor, slotsByScheduledStep, failedParticipants);
            if (action.kind() == FaultScenarioActionKind.FORWARD) {
                ForwardFaultSlot slot = slotsById.get(action.sourceFaultSlotId());
                NormalActionRef expected = normalCursor < workloadPlan.normalSchedule().size()
                        ? workloadPlan.normalSchedule().get(normalCursor) : null;
                if (slot == null || expected == null || expected.kind() != NormalActionKind.FORWARD
                        || !Objects.equals(expected.scheduledStepId(), slot.scheduledStepId())) {
                    diagnostics.add(new Diagnostic("RESIDUAL_FORWARD_ORDER_VIOLATION", action.deterministicId()));
                    continue;
                }
                int assignedBit = scenario.assignedVector().charAt(slot.slotIndex()) - '0';
                if (assignedBit == 1) {
                    failedParticipants.add(slot.sagaInstanceId());
                    List<CompensationCheckpoint> reverse = new ArrayList<>(
                            completedCheckpoints.getOrDefault(slot.sagaInstanceId(), List.of()));
                    Collections.reverse(reverse);
                    enabledRecovery.put(slot.sagaInstanceId(), reverse);
                } else {
                    successfulScheduledSteps.add(slot.scheduledStepId());
                    CompensationCheckpoint checkpoint = checkpointsBySourceStep.get(slot.scheduledStepId());
                    if (checkpoint != null) {
                        completedCheckpoints.computeIfAbsent(slot.sagaInstanceId(), ignored -> new ArrayList<>())
                                .add(checkpoint);
                    }
                }
                normalCursor++;
            } else if (action.kind() == FaultScenarioActionKind.EVENT_CONSEQUENCE) {
                NormalActionRef expected = normalCursor < workloadPlan.normalSchedule().size()
                        ? workloadPlan.normalSchedule().get(normalCursor) : null;
                if (expected == null || expected.kind() != NormalActionKind.EVENT_CONSEQUENCE
                        || !Objects.equals(expected.eventConsequenceId(), action.sourceEventConsequenceId())) {
                    diagnostics.add(new Diagnostic("RESIDUAL_NORMAL_ORDER_VIOLATION", action.deterministicId()));
                } else {
                    normalCursor++;
                }
            } else if (action.kind() == FaultScenarioActionKind.COMPENSATION) {
                if (nextNormalIsMaskedConsequence(workloadPlan.normalSchedule(), normalCursor,
                        consequencesById, successfulScheduledSteps)) {
                    diagnostics.add(new Diagnostic("MASKED_EVENT_ORDER_VIOLATION", action.deterministicId()));
                }
                CompensationCheckpoint checkpoint = checkpointsById.get(action.sourceCompensationCheckpointId());
                if (checkpoint == null) {
                    continue;
                }
                List<CompensationCheckpoint> queue = enabledRecovery.get(action.sagaInstanceId());
                if (queue == null || queue.isEmpty()) {
                    diagnostics.add(new Diagnostic("COMPENSATION_NOT_ENABLED", action.deterministicId()));
                } else if (!Objects.equals(queue.get(0).deterministicId(), checkpoint.deterministicId())) {
                    diagnostics.add(new Diagnostic("COMPENSATION_ORDER_VIOLATION", action.deterministicId()));
                } else {
                    queue.remove(0);
                }
            }
        }

        normalCursor = skipFailedOwnerForwards(
                workloadPlan.normalSchedule(), normalCursor, slotsByScheduledStep, failedParticipants);
        if (normalCursor != workloadPlan.normalSchedule().size()) {
            diagnostics.add(new Diagnostic("INCOMPLETE_RESIDUAL_NORMAL_SEQUENCE",
                    "normal sequence ended at action " + normalCursor));
        }
        enabledRecovery.forEach((participantId, queue) -> {
            if (!queue.isEmpty()) {
                diagnostics.add(new Diagnostic("INCOMPLETE_COMPENSATION_SEQUENCE", participantId));
            }
        });
    }

    private int skipFailedOwnerForwards(List<NormalActionRef> normalSchedule,
                                        int cursor,
                                        Map<String, ForwardFaultSlot> slotsByScheduledStep,
                                        Set<String> failedParticipants) {
        int next = cursor;
        while (next < normalSchedule.size()) {
            NormalActionRef action = normalSchedule.get(next);
            if (action.kind() != NormalActionKind.FORWARD) {
                break;
            }
            ForwardFaultSlot slot = slotsByScheduledStep.get(action.scheduledStepId());
            if (slot == null || !failedParticipants.contains(slot.sagaInstanceId())) {
                break;
            }
            next++;
        }
        return next;
    }

    private boolean nextNormalIsMaskedConsequence(List<NormalActionRef> normalSchedule,
                                                   int cursor,
                                                   Map<String, EventConsequence> consequencesById,
                                                   Set<String> successfulScheduledSteps) {
        if (cursor >= normalSchedule.size()) {
            return false;
        }
        NormalActionRef next = normalSchedule.get(cursor);
        if (next.kind() != NormalActionKind.EVENT_CONSEQUENCE) {
            return false;
        }
        EventConsequence consequence = consequencesById.get(next.eventConsequenceId());
        return consequence != null
                && !successfulScheduledSteps.contains(consequence.triggerScheduledStepId());
    }

    public record ValidationResult(boolean valid, List<Diagnostic> diagnostics) {
        public ValidationResult {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    public record Diagnostic(String code, String message) {
    }
}
