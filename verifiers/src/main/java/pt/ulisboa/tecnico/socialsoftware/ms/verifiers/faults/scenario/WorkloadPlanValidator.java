package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadExecutionShape;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class WorkloadPlanValidator {

    public ValidationResult validate(WorkloadPlan plan) {
        List<Diagnostic> diagnostics = new ArrayList<>();
        if (plan == null) {
            return new ValidationResult(false, List.of(new Diagnostic("MISSING_WORKLOAD_PLAN", "workload plan is required")));
        }
        if (!WorkloadPlan.SCHEMA_VERSION.equals(plan.schemaVersion())) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_WORKLOAD_SCHEMA",
                    "expected " + WorkloadPlan.SCHEMA_VERSION + " but found " + plan.schemaVersion()));
        }
        if (plan.executionShape() != WorkloadExecutionShape.SAGA_LOCAL) {
            diagnostics.add(new Diagnostic("UNSUPPORTED_EXECUTION_SHAPE", "only SAGA_LOCAL workloads are supported"));
        }
        if (plan.participants().isEmpty()) {
            diagnostics.add(new Diagnostic("MISSING_PARTICIPANTS", "workload must contain at least one participant"));
        } else if (plan.kind() == ScenarioKind.SINGLE_SAGA && plan.participants().size() != 1) {
            diagnostics.add(new Diagnostic("INVALID_SINGLE_SAGA_SHAPE", "single-saga workload must contain exactly one participant"));
        } else if (plan.kind() == ScenarioKind.MULTI_SAGA && plan.participants().size() < 2) {
            diagnostics.add(new Diagnostic("INVALID_MULTI_SAGA_SHAPE", "multi-saga workload must contain at least two participants"));
        }

        Map<String, SagaInstance> participantsById = indexParticipants(plan.participants(), diagnostics);
        Map<String, InputVariant> inputsById = indexInputs(plan.acceptedInputs(), diagnostics);
        for (SagaInstance participant : plan.participants()) {
            if (participant == null) {
                continue;
            }
            InputVariant input = inputsById.get(participant.inputVariantId());
            if (input == null) {
                diagnostics.add(new Diagnostic("MISSING_PARTICIPANT_INPUT",
                        "participant " + participant.deterministicId() + " references missing input " + participant.inputVariantId()));
            } else if (!Objects.equals(participant.sagaFqn(), input.sagaFqn())) {
                diagnostics.add(new Diagnostic("MISOWNED_PARTICIPANT_INPUT",
                        "participant " + participant.deterministicId() + " and input " + input.deterministicId() + " have different saga FQNs"));
            }
        }

        Map<String, ScheduledStep> stepsById = indexForwardSchedule(plan.forwardSchedule(), participantsById, diagnostics);
        validateConflicts(plan.conflictEvidence(), stepsById, diagnostics);
        validateFaultSlots(plan.faultSlots(), plan.forwardSchedule(), stepsById, diagnostics);
        validateCheckpoints(plan.compensationCheckpoints(), stepsById, participantsById, diagnostics);

        String expectedId = ScenarioIdGenerator.workloadPlanId(plan);
        if (plan.deterministicId() == null) {
            diagnostics.add(new Diagnostic("MISSING_WORKLOAD_ID", "workload deterministicId is required"));
        } else if (!plan.deterministicId().equals(expectedId)) {
            diagnostics.add(new Diagnostic("WORKLOAD_ID_MISMATCH",
                    "workload deterministicId does not match its semantic content"));
        }
        return new ValidationResult(diagnostics.isEmpty(), diagnostics);
    }

    private Map<String, SagaInstance> indexParticipants(List<SagaInstance> participants, List<Diagnostic> diagnostics) {
        Map<String, SagaInstance> byId = new LinkedHashMap<>();
        for (SagaInstance participant : participants) {
            if (participant == null || participant.deterministicId() == null) {
                diagnostics.add(new Diagnostic("MISSING_PARTICIPANT_ID", "every participant requires a deterministic id"));
                continue;
            }
            if (byId.putIfAbsent(participant.deterministicId(), participant) != null) {
                diagnostics.add(new Diagnostic("DUPLICATE_PARTICIPANT_ID", participant.deterministicId()));
            }
        }
        return byId;
    }

    private Map<String, InputVariant> indexInputs(List<InputVariant> inputs, List<Diagnostic> diagnostics) {
        Map<String, InputVariant> byId = new LinkedHashMap<>();
        for (InputVariant input : inputs) {
            if (input == null || input.deterministicId() == null) {
                diagnostics.add(new Diagnostic("MISSING_INPUT_ID", "every accepted input requires a deterministic id"));
                continue;
            }
            if (byId.putIfAbsent(input.deterministicId(), input) != null) {
                diagnostics.add(new Diagnostic("DUPLICATE_INPUT_ID", input.deterministicId()));
            }
            if (input.inputRecipe() != null && !input.inputRecipe().fingerprintMatchesSemanticContent()) {
                diagnostics.add(new Diagnostic("INPUT_RECIPE_FINGERPRINT_MISMATCH",
                        "input " + input.deterministicId() + " recipeFingerprint does not match its semantic content"));
            }
        }
        return byId;
    }

    private Map<String, ScheduledStep> indexForwardSchedule(List<ScheduledStep> schedule,
                                                             Map<String, SagaInstance> participantsById,
                                                             List<Diagnostic> diagnostics) {
        Map<String, ScheduledStep> byId = new LinkedHashMap<>();
        Map<ParticipantRuntimeStepKey, ScheduledStep> firstRuntimeStepOccurrences = new LinkedHashMap<>();
        for (int index = 0; index < schedule.size(); index++) {
            ScheduledStep step = schedule.get(index);
            if (step == null || step.deterministicId() == null) {
                diagnostics.add(new Diagnostic("MISSING_FORWARD_OCCURRENCE_ID", "forward step at index " + index + " requires an occurrence id"));
                continue;
            }
            if (byId.putIfAbsent(step.deterministicId(), step) != null) {
                diagnostics.add(new Diagnostic("DUPLICATE_FORWARD_OCCURRENCE_ID", step.deterministicId()));
            }
            if (step.scheduleOrder() != index) {
                diagnostics.add(new Diagnostic("INVALID_FORWARD_ORDER",
                        "forward step " + step.deterministicId() + " has scheduleOrder " + step.scheduleOrder() + " but appears at index " + index));
            }
            if (!participantsById.containsKey(step.sagaInstanceId())) {
                diagnostics.add(new Diagnostic("UNKNOWN_FORWARD_OWNER",
                        "forward step " + step.deterministicId() + " references " + step.sagaInstanceId()));
            }
            String expectedRuntimeName = ScheduledStep.runtimeStepNameFromStepId(step.stepId());
            boolean validRuntimeMapping = expectedRuntimeName != null
                    && Objects.equals(expectedRuntimeName, step.runtimeStepName());
            if (!validRuntimeMapping) {
                diagnostics.add(new Diagnostic("INVALID_RUNTIME_STEP_MAPPING",
                        "forward step " + step.deterministicId() + " has invalid runtime step name mapping"));
            }
            if (participantsById.containsKey(step.sagaInstanceId()) && validRuntimeMapping) {
                ParticipantRuntimeStepKey key = new ParticipantRuntimeStepKey(
                        step.sagaInstanceId(), step.runtimeStepName());
                ScheduledStep firstOccurrence = firstRuntimeStepOccurrences.putIfAbsent(key, step);
                if (firstOccurrence != null) {
                    diagnostics.add(new Diagnostic("DUPLICATE_PARTICIPANT_RUNTIME_STEP_NAME",
                            "participant " + step.sagaInstanceId() + " repeats runtime step name "
                                    + step.runtimeStepName() + ": first occurrence "
                                    + firstOccurrence.deterministicId() + ", repeated occurrence "
                                    + step.deterministicId()));
                }
            }
        }
        return byId;
    }

    private void validateConflicts(List<ConflictEvidence> conflicts,
                                   Map<String, ScheduledStep> stepsById,
                                   List<Diagnostic> diagnostics) {
        Set<String> ids = new HashSet<>();
        for (ConflictEvidence conflict : conflicts) {
            if (conflict == null || conflict.deterministicId() == null || !ids.add(conflict.deterministicId())) {
                diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_CONFLICT_ID", conflict == null ? "null" : conflict.deterministicId()));
                continue;
            }
            if (!stepsById.containsKey(conflict.leftScheduledStepId()) || !stepsById.containsKey(conflict.rightScheduledStepId())) {
                diagnostics.add(new Diagnostic("DANGLING_CONFLICT_REFERENCE", conflict.deterministicId()));
            }
        }
    }

    private void validateFaultSlots(List<ForwardFaultSlot> faultSlots,
                                    List<ScheduledStep> schedule,
                                    Map<String, ScheduledStep> stepsById,
                                    List<Diagnostic> diagnostics) {
        if (faultSlots.size() != schedule.size()) {
            diagnostics.add(new Diagnostic("FAULT_SPACE_SHAPE_MISMATCH",
                    "fault slot count must equal forward schedule occurrence count"));
        }
        Set<String> slotIds = new HashSet<>();
        for (int index = 0; index < faultSlots.size(); index++) {
            ForwardFaultSlot slot = faultSlots.get(index);
            if (slot == null || slot.deterministicId() == null || !slotIds.add(slot.deterministicId())) {
                diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_FAULT_SLOT_ID", slot == null ? "null" : slot.deterministicId()));
                continue;
            }
            ScheduledStep source = stepsById.get(slot.scheduledStepId());
            if (slot.slotIndex() != index || source == null
                    || !Objects.equals(slot.occurrenceId(), slot.scheduledStepId())
                    || !sameOccurrence(slot, source)) {
                diagnostics.add(new Diagnostic("MALFORMED_FAULT_SLOT", slot.deterministicId()));
            }
            if (index < schedule.size() && !Objects.equals(schedule.get(index).deterministicId(), slot.scheduledStepId())) {
                diagnostics.add(new Diagnostic("FAULT_SLOT_ORDER_MISMATCH", slot.deterministicId()));
            }
        }
    }

    private boolean sameOccurrence(ForwardFaultSlot slot, ScheduledStep step) {
        return Objects.equals(slot.sagaInstanceId(), step.sagaInstanceId())
                && Objects.equals(slot.stepId(), step.stepId())
                && Objects.equals(slot.runtimeStepName(), step.runtimeStepName());
    }

    private void validateCheckpoints(List<CompensationCheckpoint> checkpoints,
                                     Map<String, ScheduledStep> stepsById,
                                     Map<String, SagaInstance> participantsById,
                                     List<Diagnostic> diagnostics) {
        Set<String> checkpointIds = new HashSet<>();
        Set<String> sourceIds = new HashSet<>();
        int previousSourceOrder = -1;
        for (int index = 0; index < checkpoints.size(); index++) {
            CompensationCheckpoint checkpoint = checkpoints.get(index);
            if (checkpoint == null || checkpoint.deterministicId() == null || !checkpointIds.add(checkpoint.deterministicId())) {
                diagnostics.add(new Diagnostic("DUPLICATE_OR_MISSING_CHECKPOINT_ID", checkpoint == null ? "null" : checkpoint.deterministicId()));
                continue;
            }
            ScheduledStep source = stepsById.get(checkpoint.sourceScheduledStepId());
            boolean malformed = checkpoint.checkpointIndex() != index
                    || source == null
                    || checkpoint.evidenceClass() == null
                    || !participantsById.containsKey(checkpoint.sagaInstanceId())
                    || !sourceIds.add(checkpoint.sourceScheduledStepId())
                    || !Objects.equals(checkpoint.occurrenceId(), checkpoint.sourceScheduledStepId())
                    || (source != null && (!Objects.equals(checkpoint.sagaInstanceId(), source.sagaInstanceId())
                    || !Objects.equals(checkpoint.stepId(), source.stepId())
                    || !Objects.equals(checkpoint.runtimeStepName(), source.runtimeStepName())));
            if (malformed) {
                diagnostics.add(new Diagnostic("MALFORMED_COMPENSATION_CHECKPOINT", checkpoint.deterministicId()));
            }
            if (source != null && source.scheduleOrder() < previousSourceOrder) {
                diagnostics.add(new Diagnostic("CHECKPOINT_ORDER_MISMATCH", checkpoint.deterministicId()));
            }
            if (source != null) {
                previousSourceOrder = source.scheduleOrder();
            }
        }
    }

    private record ParticipantRuntimeStepKey(String sagaInstanceId, String runtimeStepName) {
    }

    public record ValidationResult(boolean valid, List<Diagnostic> diagnostics) {
        public ValidationResult {
            diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
        }
    }

    public record Diagnostic(String code, String message) {
    }
}
