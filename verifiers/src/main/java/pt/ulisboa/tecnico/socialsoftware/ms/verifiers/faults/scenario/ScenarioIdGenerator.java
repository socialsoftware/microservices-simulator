package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AccessMode;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.AggregateKey;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.BaselineBindingRequirement;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ConflictKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventConsequence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EventEmissionSite;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FootprintConfidence;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputOwner;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.NormalActionRef;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.PrerequisiteBaseline;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputResolutionStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepFootprint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupArgument;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupParticipantBinding;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupPropertyAssignment;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SetupValueRecipe;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;

public final class ScenarioIdGenerator {

    private static final HexFormat HEX = HexFormat.of();
    private static final Comparator<String> STRING_ORDER = Comparator.nullsFirst(String::compareTo);

    private ScenarioIdGenerator() {
    }

    public static String inputVariantId(String sagaFqn,
                                        String sourceClassFqn,
                                        String sourceMethodName,
                                        String sourceBindingName,
                                        InputResolutionStatus resolutionStatus,
                                         String stableSourceText,
                                         String provenanceText,
                                         List<String> constructorArgumentSummaries,
                                         Map<String, String> logicalKeyBindings) {
        return inputVariantId(sagaFqn,
                sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                resolutionStatus,
                stableSourceText,
                provenanceText,
                constructorArgumentSummaries,
                logicalKeyBindings,
                null);
    }

    public static String inputVariantId(String sagaFqn,
                                        String sourceClassFqn,
                                        String sourceMethodName,
                                        String sourceBindingName,
                                        InputResolutionStatus resolutionStatus,
                                        String stableSourceText,
                                        String provenanceText,
                                        List<String> constructorArgumentSummaries,
                                        Map<String, String> logicalKeyBindings,
                                        String recipeFingerprint) {
        return hash(digest -> {
            updateString(digest, "input-variant");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(sourceClassFqn));
            updateString(digest, normalize(sourceMethodName));
            updateString(digest, normalize(sourceBindingName));
            updateString(digest, resolutionStatus == null ? null : resolutionStatus.name());
            updateString(digest, normalize(stableSourceText));
            updateString(digest, normalize(provenanceText));
            updateStrings(digest, sortedStrings(constructorArgumentSummaries));
            updateMap(digest, logicalKeyBindings);
            if (normalize(recipeFingerprint) != null) {
                updateString(digest, normalize(recipeFingerprint));
            }
        });
    }

    public static String sagaInstanceId(String sagaFqn, String inputVariantId) {
        return hash(digest -> {
            updateString(digest, "saga-instance");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(inputVariantId));
        });
    }

    public static String stepDefinitionId(String sagaFqn, StepDefinition step) {
        String provided = normalize(step == null ? null : step.deterministicId());
        if (provided != null) {
            return provided;
        }

        return hash(digest -> {
            updateString(digest, "step-definition");
            updateString(digest, normalize(sagaFqn));
            updateString(digest, normalize(step == null ? null : step.stepKey()));
            updateString(digest, normalize(step == null ? null : step.name()));
            updateInt(digest, step == null ? 0 : step.orderIndex());
            updateStrings(digest, sortedStrings(step == null ? List.of() : step.predecessorStepKeys()));
            updateFootprints(digest, step == null ? List.of() : step.footprints());
        });
    }

    public static String scheduledStepId(String sagaInstanceId, String stepId, int scheduleOrder) {
        return hash(digest -> {
            updateString(digest, "scheduled-step");
            updateString(digest, normalize(sagaInstanceId));
            updateString(digest, normalize(stepId));
            updateInt(digest, scheduleOrder);
        });
    }

    public static String conflictEvidenceId(String leftScheduledStepId,
                                            String rightScheduledStepId,
                                            AggregateKey leftAggregateKey,
                                            AggregateKey rightAggregateKey,
                                            AccessMode leftAccessMode,
                                            AccessMode rightAccessMode,
                                            ConflictKind kind) {
        return hash(digest -> {
            updateString(digest, "conflict-evidence");
            updateString(digest, normalize(leftScheduledStepId));
            updateString(digest, normalize(rightScheduledStepId));
            updateAggregateKey(digest, leftAggregateKey);
            updateAggregateKey(digest, rightAggregateKey);
            updateString(digest, leftAccessMode == null ? null : leftAccessMode.name());
            updateString(digest, rightAccessMode == null ? null : rightAccessMode.name());
            updateString(digest, kind == null ? null : kind.name());
        });
    }

    public static String workloadPlanId(WorkloadPlan workloadPlan) {
        WorkloadPlan plan = Objects.requireNonNull(workloadPlan, "workloadPlan");
        return hash(digest -> {
            updateString(digest, "workload-plan");
            updateString(digest, plan.schemaVersion());
            updateString(digest, plan.kind() == null ? null : plan.kind().name());
            updateString(digest, plan.executionShape() == null ? null : plan.executionShape().name());
            updateSagaInstances(digest, plan.participants());
            updateInputVariants(digest, plan.acceptedInputs());
            updateScheduledSteps(digest, plan.forwardSchedule());
            updateEventConsequences(digest, plan.eventConsequences());
            updateNormalSchedule(digest, plan.normalSchedule());
            updatePrerequisiteBaseline(digest, plan.prerequisiteBaseline());
            if (!WorkloadPlan.LEGACY_V4_SCHEMA_VERSION.equals(plan.schemaVersion())) {
                updateSetupPlan(digest, plan.setupPlan());
            }
            updateConflictEvidence(digest, plan.conflictEvidence());
            updateFaultSlots(digest, plan.faultSlots());
            updateCompensationCheckpoints(digest, plan.compensationCheckpoints());
        });
    }

    public static String workloadPlanId(pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioKind kind,
                                        String executionShape,
                                        List<SagaInstance> participants,
                                        List<InputVariant> acceptedInputs,
                                        List<ScheduledStep> forwardSchedule,
                                        List<ConflictEvidence> conflictEvidence,
                                        List<ForwardFaultSlot> faultSlots,
                                        List<CompensationCheckpoint> compensationCheckpoints) {
        return hash(digest -> {
            updateString(digest, "workload-plan");
            updateString(digest, WorkloadPlan.SCHEMA_VERSION);
            updateString(digest, kind == null ? null : kind.name());
            updateString(digest, executionShape);
            updateSagaInstances(digest, participants);
            updateInputVariants(digest, acceptedInputs);
            updateScheduledSteps(digest, forwardSchedule);
            updateEventConsequences(digest, List.of());
            updateNormalSchedule(digest, forwardSchedule == null ? List.of() : java.util.stream.IntStream.range(0, forwardSchedule.size())
                    .mapToObj(index -> NormalActionRef.forward(index, forwardSchedule.get(index).deterministicId()))
                    .toList());
            updatePrerequisiteBaseline(digest, null);
            updateSetupPlan(digest, null);
            updateConflictEvidence(digest, conflictEvidence);
            updateFaultSlots(digest, faultSlots);
            updateCompensationCheckpoints(digest, compensationCheckpoints);
        });
    }

    public static String forwardFaultSlotId(int slotIndex, ScheduledStep step) {
        return hash(digest -> {
            updateString(digest, "forward-fault-slot");
            updateInt(digest, slotIndex);
            updateScheduledStep(digest, step);
        });
    }

    public static String compensationCheckpointId(int checkpointIndex,
                                                  ScheduledStep step,
                                                  StepDefinition definition) {
        return hash(digest -> {
            updateString(digest, "compensation-checkpoint");
            updateInt(digest, checkpointIndex);
            updateScheduledStep(digest, step);
            updateString(digest, definition == null || definition.compensationEvidence() == null
                    ? null
                    : definition.compensationEvidence().name());
            updateFootprints(digest, definition == null ? List.of() : definition.footprints());
            updateFootprints(digest, definition == null ? List.of() : definition.compensationFootprints());
        });
    }

    public static String eventEmissionSiteId(String serviceClassFqn,
                                             String serviceMethodSignature,
                                             int emissionOrdinal,
                                             String eventTypeFqn) {
        return hash(digest -> {
            updateString(digest, "event-emission-site");
            updateString(digest, normalize(serviceClassFqn));
            updateString(digest, normalize(serviceMethodSignature));
            updateInt(digest, emissionOrdinal);
            updateString(digest, normalize(eventTypeFqn));
        });
    }

    public static String eventConsequenceId(String triggerScheduledStepId,
                                            EventEmissionSite site,
                                            String eventHandlingClassFqn,
                                            String eventHandlingMethodName,
                                            String eventHandlerClassFqn,
                                            String eventProcessingClassFqn,
                                            String eventProcessingMethodName,
                                            String facadeClassFqn,
                                            String facadeMethodName,
                                            String downstreamSagaFqn,
                                            String deliveryPolicy) {
        return hash(digest -> {
            updateString(digest, "event-consequence");
            updateString(digest, normalize(triggerScheduledStepId));
            updateEventEmissionSite(digest, site);
            updateString(digest, normalize(eventHandlingClassFqn));
            updateString(digest, normalize(eventHandlingMethodName));
            updateString(digest, normalize(eventHandlerClassFqn));
            updateString(digest, normalize(eventProcessingClassFqn));
            updateString(digest, normalize(eventProcessingMethodName));
            updateString(digest, normalize(facadeClassFqn));
            updateString(digest, normalize(facadeMethodName));
            updateString(digest, normalize(downstreamSagaFqn));
            updateString(digest, normalize(deliveryPolicy));
        });
    }

    public static String faultScenarioActionId(FaultScenarioActionKind kind,
                                               String sagaInstanceId,
                                               String sourceFaultSlotId,
                                               String sourceCompensationCheckpointId,
                                               String occurrenceId) {
        return faultScenarioActionId(kind, sagaInstanceId, sourceFaultSlotId,
                sourceCompensationCheckpointId, null, occurrenceId);
    }

    public static String faultScenarioActionId(FaultScenarioActionKind kind,
                                               String sagaInstanceId,
                                               String sourceFaultSlotId,
                                               String sourceCompensationCheckpointId,
                                               String sourceEventConsequenceId,
                                               String occurrenceId) {
        return hash(digest -> {
            updateString(digest, "fault-scenario-action");
            updateFaultScenarioActionFields(digest, kind, sagaInstanceId, sourceFaultSlotId,
                    sourceCompensationCheckpointId, sourceEventConsequenceId, occurrenceId);
        });
    }

    public static String faultScenarioActionId(FaultScenarioAction action) {
        FaultScenarioAction value = Objects.requireNonNull(action, "action");
        return faultScenarioActionId(value.kind(), value.sagaInstanceId(), value.sourceFaultSlotId(),
                value.sourceCompensationCheckpointId(), value.sourceEventConsequenceId(), value.occurrenceId());
    }

    public static String faultScenarioId(FaultScenario faultScenario) {
        FaultScenario scenario = Objects.requireNonNull(faultScenario, "faultScenario");
        return faultScenarioId(scenario.workloadPlanId(), scenario.assignedVector(), scenario.actions());
    }

    public static String faultScenarioId(String workloadPlanId,
                                         String assignedVector,
                                         List<FaultScenarioAction> actions) {
        return hash(digest -> {
            updateString(digest, "fault-scenario");
            updateString(digest, FaultScenario.SCHEMA_VERSION);
            updateString(digest, normalize(workloadPlanId));
            updateString(digest, assignedVector);
            List<FaultScenarioAction> ordered = actions == null ? List.of() : actions;
            updateInt(digest, ordered.size());
            for (FaultScenarioAction action : ordered) {
                updateFaultScenarioActionFields(
                        digest,
                        action == null ? null : action.kind(),
                        action == null ? null : action.sagaInstanceId(),
                        action == null ? null : action.sourceFaultSlotId(),
                        action == null ? null : action.sourceCompensationCheckpointId(),
                        action == null ? null : action.sourceEventConsequenceId(),
                        action == null ? null : action.occurrenceId());
            }
        });
    }

    private static void updateFaultScenarioActionFields(MessageDigest digest,
                                                        FaultScenarioActionKind kind,
                                                        String sagaInstanceId,
                                                        String sourceFaultSlotId,
                                                        String sourceCompensationCheckpointId,
                                                        String sourceEventConsequenceId,
                                                        String occurrenceId) {
        updateString(digest, kind == null ? null : kind.name());
        updateString(digest, normalize(sagaInstanceId));
        updateString(digest, normalize(sourceFaultSlotId));
        updateString(digest, normalize(sourceCompensationCheckpointId));
        updateString(digest, normalize(sourceEventConsequenceId));
        updateString(digest, normalize(occurrenceId));
    }

    private static void updateSagaInstances(MessageDigest digest, List<SagaInstance> sagaInstances) {
        List<SagaInstance> ordered = sagaInstances == null ? List.of() : sagaInstances;
        updateInt(digest, ordered.size());
        for (SagaInstance sagaInstance : ordered) {
            updateString(digest, sagaInstance == null ? null : sagaInstance.deterministicId());
            updateString(digest, sagaInstance == null ? null : sagaInstance.sagaFqn());
            updateString(digest, sagaInstance == null ? null : sagaInstance.inputVariantId());
        }
    }

    private static void updateInputVariants(MessageDigest digest, List<InputVariant> inputs) {
        List<InputVariant> ordered = inputs == null ? List.of() : inputs;
        updateInt(digest, ordered.size());
        for (InputVariant input : ordered) {
            updateString(digest, input == null ? null : input.deterministicId());
            updateString(digest, input == null ? null : input.sagaFqn());
            updateString(digest, input == null ? null : input.sourceClassFqn());
            updateString(digest, input == null ? null : input.sourceMethodName());
            updateString(digest, input == null ? null : input.sourceBindingName());
            updateString(digest, input == null ? null : input.callContextMethodName());
            updateString(digest, input == null || input.inputRole() == null ? null : input.inputRole().name());
            updateString(digest, input == null || input.fixtureOrigin() == null ? null : input.fixtureOrigin().name());
            updateString(digest, input == null || input.resolutionStatus() == null ? null : input.resolutionStatus().name());
            updateString(digest, input == null || input.sourceMode() == null ? null : input.sourceMode().name());
            updateString(digest, input == null || input.sourceModeConfidence() == null ? null : input.sourceModeConfidence().name());
            updateStrings(digest, input == null ? List.of() : input.sourceModeEvidence());
            updateString(digest, input == null ? null : input.stableSourceText());
            updateString(digest, input == null ? null : input.provenanceText());
            updateInputOwners(digest, input == null ? List.of() : input.owners());
            updateStrings(digest, input == null ? List.of() : input.constructorArgumentSummaries());
            updateMap(digest, input == null ? Map.of() : input.logicalKeyBindings());
            updateString(digest, input == null || input.inputRecipe() == null ? null : input.inputRecipe().schemaVersion());
            updateString(digest, input == null || input.inputRecipe() == null ? null : input.inputRecipe().semanticFingerprint());
            updateString(digest, input == null || input.inputRecipe() == null ? null : Boolean.toString(input.inputRecipe().executorReady()));
        }
    }

    private static void updateInputOwners(MessageDigest digest, List<InputOwner> owners) {
        List<InputOwner> sorted = owners == null ? List.of() : owners.stream()
                .sorted(Comparator.comparing(InputOwner::testClassFqn, STRING_ORDER)
                        .thenComparing(InputOwner::testMethodName, STRING_ORDER))
                .toList();
        updateInt(digest, sorted.size());
        for (InputOwner owner : sorted) {
            updateString(digest, owner.testClassFqn());
            updateString(digest, owner.testMethodName());
        }
    }

    private static void updateScheduledSteps(MessageDigest digest, List<ScheduledStep> steps) {
        List<ScheduledStep> ordered = steps == null ? List.of() : steps;
        updateInt(digest, ordered.size());
        for (ScheduledStep step : ordered) {
            updateScheduledStep(digest, step);
        }
    }

    private static void updateScheduledStep(MessageDigest digest, ScheduledStep step) {
        updateString(digest, step == null ? null : step.deterministicId());
        updateString(digest, step == null ? null : step.sagaInstanceId());
        updateString(digest, step == null ? null : step.stepId());
        updateInt(digest, step == null ? -1 : step.scheduleOrder());
        updateString(digest, step == null ? null : step.runtimeStepName());
    }

    private static void updateEventConsequences(MessageDigest digest, List<EventConsequence> consequences) {
        List<EventConsequence> ordered = consequences == null ? List.of() : consequences;
        updateInt(digest, ordered.size());
        for (EventConsequence consequence : ordered) {
            updateString(digest, consequence == null ? null : consequence.deterministicId());
            updateString(digest, consequence == null ? null : consequence.triggerScheduledStepId());
            updateEventEmissionSite(digest, consequence == null ? null : consequence.emissionSite());
            updateString(digest, consequence == null ? null : consequence.eventTypeFqn());
            updateString(digest, consequence == null ? null : consequence.eventHandlingClassFqn());
            updateString(digest, consequence == null ? null : consequence.eventHandlingMethodName());
            updateString(digest, consequence == null ? null : consequence.eventHandlerClassFqn());
            updateString(digest, consequence == null ? null : consequence.eventProcessingClassFqn());
            updateString(digest, consequence == null ? null : consequence.eventProcessingMethodName());
            updateString(digest, consequence == null ? null : consequence.facadeClassFqn());
            updateString(digest, consequence == null ? null : consequence.facadeMethodName());
            updateString(digest, consequence == null ? null : consequence.downstreamSagaFqn());
            updateString(digest, consequence == null ? null : consequence.deliveryPolicy());
        }
    }

    private static void updateEventEmissionSite(MessageDigest digest, EventEmissionSite site) {
        updateString(digest, site == null ? null : site.deterministicId());
        updateString(digest, site == null ? null : site.sourceServiceClassFqn());
        updateString(digest, site == null ? null : site.sourceServiceMethodSignature());
        updateInt(digest, site == null ? -1 : site.emissionOrdinal());
        updateString(digest, site == null ? null : site.eventTypeFqn());
    }

    private static void updateNormalSchedule(MessageDigest digest, List<NormalActionRef> normalSchedule) {
        List<NormalActionRef> ordered = normalSchedule == null ? List.of() : normalSchedule;
        updateInt(digest, ordered.size());
        for (NormalActionRef action : ordered) {
            updateInt(digest, action == null ? -1 : action.normalOrder());
            updateString(digest, action == null || action.kind() == null ? null : action.kind().name());
            updateString(digest, action == null ? null : action.scheduledStepId());
            updateString(digest, action == null ? null : action.eventConsequenceId());
        }
    }

    private static void updatePrerequisiteBaseline(MessageDigest digest, PrerequisiteBaseline baseline) {
        updateString(digest, baseline == null ? null : baseline.providerId());
        updateString(digest, baseline == null ? null : baseline.providerVersion());
        List<BaselineBindingRequirement> bindings = baseline == null ? List.of() : baseline.requiredBindings();
        updateInt(digest, bindings.size());
        for (BaselineBindingRequirement binding : bindings) {
            updateString(digest, binding == null ? null : binding.key());
            updateString(digest, binding == null ? null : binding.typeFqn());
        }
    }

    private static void updateSetupPlan(MessageDigest digest, SetupPlan plan) {
        updateString(digest, plan == null ? null : plan.schemaVersion());
        List<SetupAction> actions = plan == null ? List.of() : plan.actions();
        updateInt(digest, actions.size());
        for (SetupAction action : actions) {
            updateString(digest, action == null ? null : action.actionId());
            updateInt(digest, action == null ? -1 : action.orderIndex());
            updateString(digest, action == null ? null : action.sourceOccurrence());
            updateString(digest, action == null ? null : action.methodKey());
            updateString(digest, action == null ? null : action.declaredResultTypeFqn());
            updateString(digest, action == null ? null : Boolean.toString(action.voidResult()));
            List<SetupArgument> arguments = action == null ? List.of() : action.arguments();
            updateInt(digest, arguments.size());
            for (SetupArgument argument : arguments) {
                updateInt(digest, argument == null ? -1 : argument.index());
                updateString(digest, argument == null ? null : argument.expectedTypeFqn());
                updateSetupValue(digest, argument == null ? null : argument.value());
            }
        }
        List<SetupParticipantBinding> bindings = plan == null ? List.of() : plan.participantBindings();
        updateInt(digest, bindings.size());
        for (SetupParticipantBinding binding : bindings) {
            updateString(digest, binding == null ? null : binding.inputVariantId());
            updateInt(digest, binding == null ? -1 : binding.argumentIndex());
            updateString(digest, binding == null ? null : binding.expectedTypeFqn());
            updateSetupValue(digest, binding == null ? null : binding.value());
        }
    }

    private static void updateSetupValue(MessageDigest digest, SetupValueRecipe value) {
        updateString(digest, value == null || value.kind() == null ? null : value.kind().name());
        if (value == null) return;
        updateString(digest, value.declaredTypeFqn());
        updateString(digest, value.literalKind());
        updateString(digest, value.literalValue() == null ? null : value.literalValue().toString());
        updateString(digest, value.targetTypeFqn());
        updateString(digest, value.actionId());
        updateString(digest, value.propertyName());
        updateInt(digest, value.constructorArguments().size());
        value.constructorArguments().forEach(child -> updateSetupValue(digest, child));
        updateInt(digest, value.assignments().size());
        for (SetupPropertyAssignment assignment : value.assignments()) {
            updateInt(digest, assignment == null ? -1 : assignment.orderIndex());
            updateString(digest, assignment == null ? null : assignment.propertyName());
            updateSetupValue(digest, assignment == null ? null : assignment.value());
        }
        updateInt(digest, value.elements().size());
        value.elements().forEach(child -> updateSetupValue(digest, child));
        updateSetupValue(digest, value.receiver());
    }

    private static void updateConflictEvidence(MessageDigest digest, List<ConflictEvidence> conflictEvidence) {
        List<ConflictEvidence> sorted = conflictEvidence == null ? List.of() : conflictEvidence.stream()
                .sorted(Comparator
                        .comparing(ConflictEvidence::deterministicId, STRING_ORDER)
                        .thenComparing(ConflictEvidence::leftScheduledStepId, STRING_ORDER)
                        .thenComparing(ConflictEvidence::rightScheduledStepId, STRING_ORDER))
                .toList();

        updateInt(digest, sorted.size());
        for (ConflictEvidence evidence : sorted) {
            updateString(digest, evidence.deterministicId());
            updateString(digest, evidence.leftScheduledStepId());
            updateString(digest, evidence.rightScheduledStepId());
            updateAggregateKey(digest, evidence.leftAggregateKey());
            updateAggregateKey(digest, evidence.rightAggregateKey());
            updateString(digest, evidence.leftAccessMode() == null ? null : evidence.leftAccessMode().name());
            updateString(digest, evidence.rightAccessMode() == null ? null : evidence.rightAccessMode().name());
            updateString(digest, evidence.kind() == null ? null : evidence.kind().name());
        }
    }

    private static void updateFaultSlots(MessageDigest digest, List<ForwardFaultSlot> faultSlots) {
        List<ForwardFaultSlot> ordered = faultSlots == null ? List.of() : faultSlots;
        updateInt(digest, ordered.size());
        for (ForwardFaultSlot slot : ordered) {
            updateString(digest, slot == null ? null : slot.deterministicId());
            updateInt(digest, slot == null ? -1 : slot.slotIndex());
            updateString(digest, slot == null ? null : slot.scheduledStepId());
            updateString(digest, slot == null ? null : slot.sagaInstanceId());
            updateString(digest, slot == null ? null : slot.stepId());
            updateString(digest, slot == null ? null : slot.runtimeStepName());
            updateString(digest, slot == null ? null : slot.occurrenceId());
        }
    }

    private static void updateCompensationCheckpoints(MessageDigest digest,
                                                       List<CompensationCheckpoint> checkpoints) {
        List<CompensationCheckpoint> ordered = checkpoints == null ? List.of() : checkpoints;
        updateInt(digest, ordered.size());
        for (CompensationCheckpoint checkpoint : ordered) {
            updateString(digest, checkpoint == null ? null : checkpoint.deterministicId());
            updateInt(digest, checkpoint == null ? -1 : checkpoint.checkpointIndex());
            updateString(digest, checkpoint == null ? null : checkpoint.sagaInstanceId());
            updateString(digest, checkpoint == null ? null : checkpoint.sourceScheduledStepId());
            updateString(digest, checkpoint == null ? null : checkpoint.stepId());
            updateString(digest, checkpoint == null ? null : checkpoint.runtimeStepName());
            updateString(digest, checkpoint == null ? null : checkpoint.occurrenceId());
            updateString(digest, checkpoint == null || checkpoint.evidenceClass() == null
                    ? null
                    : checkpoint.evidenceClass().name());
            updateFootprints(digest, checkpoint == null ? List.of() : checkpoint.forwardFootprints());
            updateFootprints(digest, checkpoint == null ? List.of() : checkpoint.compensationFootprints());
        }
    }

    private static void updateStrings(MessageDigest digest, List<String> values) {
        List<String> ordered = values == null ? List.of() : values;
        updateInt(digest, ordered.size());
        for (String value : ordered) {
            updateString(digest, value);
        }
    }

    private static void updateFootprints(MessageDigest digest, List<StepFootprint> footprints) {
        List<StepFootprint> ordered = footprints == null ? List.of() : footprints.stream()
                .sorted(Comparator
                        .comparing((StepFootprint footprint) -> aggregateKeySignature(footprint == null ? null : footprint.aggregateKey()), STRING_ORDER)
                        .thenComparing(footprint -> footprint == null || footprint.accessMode() == null ? null : footprint.accessMode().name(), STRING_ORDER))
                .toList();

        updateInt(digest, ordered.size());
        for (StepFootprint footprint : ordered) {
            updateAggregateKey(digest, footprint == null ? null : footprint.aggregateKey());
            updateString(digest, footprint == null || footprint.accessMode() == null ? null : footprint.accessMode().name());
        }
    }

    private static String aggregateKeySignature(AggregateKey aggregateKey) {
        if (aggregateKey == null) {
            return "";
        }
        return safeSegment(aggregateKey.aggregateTypeName())
                + "|"
                + safeSegment(aggregateKey.aggregateName())
                + "|"
                + safeSegment(aggregateKey.keyText())
                + "|"
                + (aggregateKey.confidence() == null ? FootprintConfidence.UNKNOWN.name() : aggregateKey.confidence().name());
    }

    private static void updateAggregateKey(MessageDigest digest, AggregateKey aggregateKey) {
        updateString(digest, aggregateKey == null ? null : aggregateKey.aggregateTypeName());
        updateString(digest, aggregateKey == null ? null : aggregateKey.aggregateName());
        updateString(digest, aggregateKey == null ? null : aggregateKey.keyText());
        updateString(digest, aggregateKey == null || aggregateKey.confidence() == null ? null : aggregateKey.confidence().name());
    }

    private static void updateMap(MessageDigest digest, Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            updateInt(digest, 0);
            return;
        }

        List<Map.Entry<String, String>> entries = values.entrySet().stream()
                .map(entry -> Map.entry(normalize(entry.getKey()), normalize(entry.getValue())))
                .filter(entry -> entry.getKey() != null)
                .sorted(Map.Entry.comparingByKey(STRING_ORDER))
                .toList();

        updateInt(digest, entries.size());
        for (Map.Entry<String, String> entry : entries) {
            updateString(digest, entry.getKey());
            updateString(digest, entry.getValue());
        }
    }

    private static List<String> sortedStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(ScenarioIdGenerator::normalize)
                .filter(Objects::nonNull)
                .sorted()
                .toList();
    }

    private static String hash(Consumer<MessageDigest> updateAction) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            updateAction.accept(digest);
            return HEX.formatHex(digest.digest());
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Missing SHA-256 support", exception);
        }
    }

    private static void updateString(MessageDigest digest, String value) {
        if (value == null) {
            updateInt(digest, -1);
            return;
        }

        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        updateInt(digest, bytes.length);
        digest.update(bytes);
    }

    private static void updateInt(MessageDigest digest, int value) {
        digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(value).array());
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private static String safeSegment(String value) {
        String normalized = normalize(value);
        return normalized == null ? "" : normalized;
    }
}
