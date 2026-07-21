package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.CompensationCheckpoint;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioAction;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioActionKind;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultSlotGenerationDiagnostic;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultSlotGenerationState;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ForwardFaultSlot;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RecoveryScheduleGenerationMetrics;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RecoveryScheduleGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public final class RecoveryScheduleGenerator {

    private static final Comparator<String> STRING_ORDER = Comparator.nullsFirst(String::compareTo);
    private static final Comparator<Transition> CANONICAL_COMPENSATION_ORDER = Comparator
            .comparing((Transition transition) -> transition.action().sagaInstanceId(), STRING_ORDER)
            .thenComparing(transition -> transition.action().occurrenceId(), STRING_ORDER)
            .thenComparing(transition -> transition.action().deterministicId(), STRING_ORDER);
    private static final Comparator<Transition> LEXICOGRAPHIC_ACTION_ORDER = Comparator
            .comparing(transition -> transition.action().deterministicId(), STRING_ORDER);

    private RecoveryScheduleGenerator() {
    }

    public static RecoveryScheduleGenerationResult generate(WorkloadPlan workloadPlan, String assignedVector) {
        return generate(workloadPlan, assignedVector, RecoveryScheduleCap.defaultCap());
    }

    public static RecoveryScheduleGenerationResult generate(WorkloadPlan workloadPlan,
                                                              String assignedVector,
                                                              int recoveryScheduleCap) {
        return generate(workloadPlan, assignedVector, new RecoveryScheduleCap(recoveryScheduleCap));
    }

    public static RecoveryScheduleGenerationResult generate(WorkloadPlan workloadPlan,
                                                              String assignedVector,
                                                              RecoveryScheduleCap recoveryScheduleCap) {
        WorkloadPlan plan = validateInputs(workloadPlan, assignedVector, recoveryScheduleCap);
        PreparedPlan prepared = prepare(plan, assignedVector);
        State initialState = State.initial(prepared.recoveryQueues().size());
        ExactCounter counter = new ExactCounter(prepared);
        BigInteger uncappedCount = counter.count(initialState);
        int cap = recoveryScheduleCap.value();
        LinkedHashMap<String, FaultScenario> retained = new LinkedHashMap<>();
        MutableMetrics metrics = new MutableMetrics();

        if (uncappedCount.compareTo(BigInteger.valueOf(cap)) <= 0) {
            materializeLexicographically(plan, assignedVector, prepared, initialState,
                    uncappedCount.intValueExact(), retained, metrics);
        } else {
            LinkedHashMap<String, FaultScenario> representatives = buildRepresentatives(
                    plan, assignedVector, prepared, metrics);
            for (FaultScenario representative : representatives.values()) {
                if (retained.size() >= cap) {
                    break;
                }
                retained.put(representative.deterministicId(), representative);
            }
            if (retained.size() < cap) {
                materializeLexicographically(plan, assignedVector, prepared, initialState,
                        cap, retained, metrics);
            }
        }

        List<FaultScenario> scenarios = List.copyOf(retained.values());
        return new RecoveryScheduleGenerationResult(
                scenarios,
                uncappedCount,
                scenarios.size(),
                cap,
                prepared.faultSlotDiagnostics(),
                new RecoveryScheduleGenerationMetrics(
                        counter.statesVisited(),
                        metrics.representativeCandidatesConstructed,
                        metrics.materializedLeavesVisited));
    }

    private static WorkloadPlan validateInputs(WorkloadPlan workloadPlan,
                                               String assignedVector,
                                               RecoveryScheduleCap recoveryScheduleCap) {
        Objects.requireNonNull(recoveryScheduleCap, "recoveryScheduleCap");
        WorkloadPlanValidator.ValidationResult validation = new WorkloadPlanValidator().validate(workloadPlan);
        if (!validation.valid()) {
            throw new IllegalArgumentException("invalid WorkloadPlan for recovery generation: " + validation.diagnostics());
        }
        if (assignedVector == null
                || assignedVector.length() != workloadPlan.faultSlots().size()
                || !assignedVector.matches("[01]*")) {
            throw new IllegalArgumentException("assigned vector must be binary and match the ordered WorkloadPlan fault-slot count");
        }
        return workloadPlan;
    }

    private static PreparedPlan prepare(WorkloadPlan plan, String assignedVector) {
        Map<String, CompensationCheckpoint> checkpointsBySource = new HashMap<>();
        for (CompensationCheckpoint checkpoint : plan.compensationCheckpoints()) {
            checkpointsBySource.put(checkpoint.sourceScheduledStepId(), checkpoint);
        }
        Map<String, String> finalOccurrenceByParticipant = new HashMap<>();
        for (ScheduledStep step : plan.forwardSchedule()) {
            finalOccurrenceByParticipant.put(step.sagaInstanceId(), step.deterministicId());
        }

        Set<String> failedParticipants = new HashSet<>();
        Map<String, List<CompensationCheckpoint>> completedCheckpoints = new HashMap<>();
        List<ForwardEvent> forwardEvents = new ArrayList<>();
        List<RecoveryQueue> recoveryQueues = new ArrayList<>();
        List<FaultSlotGenerationDiagnostic> diagnostics = new ArrayList<>();

        for (int index = 0; index < plan.faultSlots().size(); index++) {
            ForwardFaultSlot slot = plan.faultSlots().get(index);
            int assignedBit = assignedVector.charAt(index) - '0';
            if (failedParticipants.contains(slot.sagaInstanceId())) {
                FaultSlotGenerationState state = assignedBit == 1
                        ? FaultSlotGenerationState.MASKED
                        : FaultSlotGenerationState.SKIPPED_AFTER_PARTICIPANT_FAILURE;
                diagnostics.add(diagnostic(slot, assignedBit, state));
                continue;
            }

            boolean assignedFault = assignedBit == 1;
            FaultScenarioAction action = forwardAction(slot);
            boolean successfulFinalBoundary = !assignedFault
                    && Objects.equals(finalOccurrenceByParticipant.get(slot.sagaInstanceId()), slot.scheduledStepId());
            forwardEvents.add(new ForwardEvent(action, successfulFinalBoundary));
            diagnostics.add(diagnostic(slot, assignedBit, assignedFault
                    ? FaultSlotGenerationState.REALIZED
                    : FaultSlotGenerationState.NOT_ASSIGNED));

            if (assignedFault) {
                failedParticipants.add(slot.sagaInstanceId());
                List<CompensationCheckpoint> completed = completedCheckpoints.getOrDefault(slot.sagaInstanceId(), List.of());
                List<FaultScenarioAction> reverseActions = new ArrayList<>(completed.size());
                for (int checkpointIndex = completed.size() - 1; checkpointIndex >= 0; checkpointIndex--) {
                    reverseActions.add(compensationAction(completed.get(checkpointIndex)));
                }
                if (!reverseActions.isEmpty()) {
                    recoveryQueues.add(new RecoveryQueue(
                            slot.sagaInstanceId(),
                            forwardEvents.size(),
                            List.copyOf(reverseActions)));
                }
            } else {
                CompensationCheckpoint checkpoint = checkpointsBySource.get(slot.scheduledStepId());
                if (checkpoint != null) {
                    completedCheckpoints.computeIfAbsent(slot.sagaInstanceId(), ignored -> new ArrayList<>())
                            .add(checkpoint);
                }
            }
        }

        ensureUniqueActionIdentities(forwardEvents, recoveryQueues);
        return new PreparedPlan(List.copyOf(forwardEvents), List.copyOf(recoveryQueues), List.copyOf(diagnostics));
    }

    private static FaultSlotGenerationDiagnostic diagnostic(ForwardFaultSlot slot,
                                                            int assignedBit,
                                                            FaultSlotGenerationState state) {
        return new FaultSlotGenerationDiagnostic(
                slot.slotIndex(),
                slot.deterministicId(),
                slot.scheduledStepId(),
                slot.sagaInstanceId(),
                assignedBit,
                state);
    }

    private static FaultScenarioAction forwardAction(ForwardFaultSlot slot) {
        String actionId = ScenarioIdGenerator.faultScenarioActionId(
                FaultScenarioActionKind.FORWARD,
                slot.sagaInstanceId(),
                slot.deterministicId(),
                null,
                slot.occurrenceId());
        return new FaultScenarioAction(
                actionId,
                FaultScenarioActionKind.FORWARD,
                slot.sagaInstanceId(),
                slot.deterministicId(),
                null,
                slot.occurrenceId());
    }

    private static FaultScenarioAction compensationAction(CompensationCheckpoint checkpoint) {
        String actionId = ScenarioIdGenerator.faultScenarioActionId(
                FaultScenarioActionKind.COMPENSATION,
                checkpoint.sagaInstanceId(),
                null,
                checkpoint.deterministicId(),
                checkpoint.occurrenceId());
        return new FaultScenarioAction(
                actionId,
                FaultScenarioActionKind.COMPENSATION,
                checkpoint.sagaInstanceId(),
                null,
                checkpoint.deterministicId(),
                checkpoint.occurrenceId());
    }

    private static void ensureUniqueActionIdentities(List<ForwardEvent> forwardEvents,
                                                     List<RecoveryQueue> recoveryQueues) {
        Set<String> actionIds = new HashSet<>();
        for (ForwardEvent event : forwardEvents) {
            if (!actionIds.add(event.action().deterministicId())) {
                throw new IllegalArgumentException("duplicate generated action identity " + event.action().deterministicId());
            }
        }
        for (RecoveryQueue queue : recoveryQueues) {
            for (FaultScenarioAction action : queue.actions()) {
                if (!actionIds.add(action.deterministicId())) {
                    throw new IllegalArgumentException("duplicate generated action identity " + action.deterministicId());
                }
            }
        }
    }

    private static LinkedHashMap<String, FaultScenario> buildRepresentatives(WorkloadPlan plan,
                                                                             String assignedVector,
                                                                             PreparedPlan prepared,
                                                                             MutableMetrics metrics) {
        List<List<FaultScenarioAction>> candidates = new ArrayList<>();
        candidates.add(constructPolicy(prepared, Policy.EARLIEST));
        candidates.add(constructPolicy(prepared, Policy.LATEST));
        candidates.add(constructPolicy(prepared, Policy.ALTERNATING_RECOVERY_FIRST));
        candidates.add(constructPolicy(prepared, Policy.ALTERNATING_FORWARD_FIRST));
        for (int index = 0; index < prepared.forwardEvents().size(); index++) {
            if (prepared.forwardEvents().get(index).successfulFinalBoundary()) {
                candidates.add(constructBoundary(prepared, index, true));
                candidates.add(constructBoundary(prepared, index, false));
            }
        }

        LinkedHashMap<String, FaultScenario> representatives = new LinkedHashMap<>();
        for (List<FaultScenarioAction> candidate : candidates) {
            metrics.representativeCandidatesConstructed++;
            if (!isCompleteValidSchedule(prepared, candidate)) {
                continue;
            }
            FaultScenario scenario = faultScenario(plan, assignedVector, candidate);
            representatives.putIfAbsent(scenario.deterministicId(), scenario);
        }
        return representatives;
    }

    private static List<FaultScenarioAction> constructPolicy(PreparedPlan prepared, Policy policy) {
        MutableState state = new MutableState(prepared.recoveryQueues().size());
        List<FaultScenarioAction> actions = new ArrayList<>();
        boolean preferCompensation = policy == Policy.ALTERNATING_RECOVERY_FIRST;
        while (!state.terminal(prepared)) {
            boolean forwardAvailable = state.forwardIndex < prepared.forwardEvents().size();
            List<Transition> compensations = enabledCompensationTransitions(prepared, state);
            boolean compensationAvailable = !compensations.isEmpty();
            boolean chooseCompensation;
            if (policy == Policy.EARLIEST) {
                chooseCompensation = compensationAvailable;
            } else if (policy == Policy.LATEST) {
                chooseCompensation = !forwardAvailable && compensationAvailable;
            } else if (forwardAvailable && compensationAvailable) {
                chooseCompensation = preferCompensation;
                preferCompensation = !preferCompensation;
            } else {
                chooseCompensation = compensationAvailable;
            }

            if (chooseCompensation) {
                Transition transition = compensations.get(0);
                actions.add(transition.action());
                state.queuePositions[transition.queueIndex()]++;
            } else if (forwardAvailable) {
                actions.add(prepared.forwardEvents().get(state.forwardIndex).action());
                state.forwardIndex++;
            } else {
                throw new IllegalStateException("recovery representative construction reached a non-terminal dead end");
            }
        }
        return List.copyOf(actions);
    }

    private static List<FaultScenarioAction> constructBoundary(PreparedPlan prepared,
                                                               int boundaryForwardIndex,
                                                               boolean beforeBoundary) {
        MutableState state = new MutableState(prepared.recoveryQueues().size());
        List<FaultScenarioAction> actions = new ArrayList<>();
        while (state.forwardIndex < boundaryForwardIndex) {
            actions.add(prepared.forwardEvents().get(state.forwardIndex).action());
            state.forwardIndex++;
        }
        if (beforeBoundary) {
            drainEnabledCompensations(prepared, state, actions);
        }
        actions.add(prepared.forwardEvents().get(state.forwardIndex).action());
        state.forwardIndex++;
        if (!beforeBoundary) {
            drainEnabledCompensations(prepared, state, actions);
        }
        completeEarliest(prepared, state, actions);
        return List.copyOf(actions);
    }

    private static void drainEnabledCompensations(PreparedPlan prepared,
                                                  MutableState state,
                                                  List<FaultScenarioAction> actions) {
        List<Transition> enabled = enabledCompensationTransitions(prepared, state);
        while (!enabled.isEmpty()) {
            Transition transition = enabled.get(0);
            actions.add(transition.action());
            state.queuePositions[transition.queueIndex()]++;
            enabled = enabledCompensationTransitions(prepared, state);
        }
    }

    private static void completeEarliest(PreparedPlan prepared,
                                         MutableState state,
                                         List<FaultScenarioAction> actions) {
        while (!state.terminal(prepared)) {
            List<Transition> enabled = enabledCompensationTransitions(prepared, state);
            if (!enabled.isEmpty()) {
                Transition transition = enabled.get(0);
                actions.add(transition.action());
                state.queuePositions[transition.queueIndex()]++;
            } else {
                actions.add(prepared.forwardEvents().get(state.forwardIndex).action());
                state.forwardIndex++;
            }
        }
    }

    private static List<Transition> enabledCompensationTransitions(PreparedPlan prepared, MutableState state) {
        List<Transition> transitions = new ArrayList<>();
        for (int queueIndex = 0; queueIndex < prepared.recoveryQueues().size(); queueIndex++) {
            RecoveryQueue queue = prepared.recoveryQueues().get(queueIndex);
            int position = state.queuePositions[queueIndex];
            if (queue.releaseForwardCount() <= state.forwardIndex && position < queue.actions().size()) {
                transitions.add(new Transition(queue.actions().get(position), queueIndex));
            }
        }
        transitions.sort(CANONICAL_COMPENSATION_ORDER);
        return transitions;
    }

    private static boolean isCompleteValidSchedule(PreparedPlan prepared, List<FaultScenarioAction> actions) {
        State state = State.initial(prepared.recoveryQueues().size());
        for (FaultScenarioAction action : actions) {
            Transition selected = availableTransitions(prepared, state).stream()
                    .filter(transition -> Objects.equals(transition.action().deterministicId(), action.deterministicId()))
                    .findFirst()
                    .orElse(null);
            if (selected == null) {
                return false;
            }
            state = apply(state, selected);
        }
        return terminal(prepared, state);
    }

    private static void materializeLexicographically(WorkloadPlan plan,
                                                     String assignedVector,
                                                     PreparedPlan prepared,
                                                     State initialState,
                                                     int targetSize,
                                                     LinkedHashMap<String, FaultScenario> retained,
                                                     MutableMetrics metrics) {
        List<FaultScenarioAction> prefix = new ArrayList<>();
        visitLexicographicLeaves(plan, assignedVector, prepared, initialState,
                prefix, targetSize, retained, metrics);
    }

    private static boolean visitLexicographicLeaves(WorkloadPlan plan,
                                                    String assignedVector,
                                                    PreparedPlan prepared,
                                                    State state,
                                                    List<FaultScenarioAction> prefix,
                                                    int targetSize,
                                                    LinkedHashMap<String, FaultScenario> retained,
                                                    MutableMetrics metrics) {
        if (terminal(prepared, state)) {
            metrics.materializedLeavesVisited++;
            FaultScenario scenario = faultScenario(plan, assignedVector, prefix);
            retained.putIfAbsent(scenario.deterministicId(), scenario);
            return retained.size() >= targetSize;
        }
        List<Transition> transitions = new ArrayList<>(availableTransitions(prepared, state));
        transitions.sort(LEXICOGRAPHIC_ACTION_ORDER);
        for (Transition transition : transitions) {
            prefix.add(transition.action());
            boolean complete = visitLexicographicLeaves(plan, assignedVector, prepared, apply(state, transition),
                    prefix, targetSize, retained, metrics);
            prefix.remove(prefix.size() - 1);
            if (complete) {
                return true;
            }
        }
        return false;
    }

    private static FaultScenario faultScenario(WorkloadPlan plan,
                                               String assignedVector,
                                               List<FaultScenarioAction> actions) {
        FaultScenario withoutId = new FaultScenario(
                FaultScenario.SCHEMA_VERSION,
                null,
                plan.deterministicId(),
                assignedVector,
                List.copyOf(actions));
        return new FaultScenario(
                withoutId.schemaVersion(),
                ScenarioIdGenerator.faultScenarioId(withoutId),
                withoutId.workloadPlanId(),
                withoutId.assignedVector(),
                withoutId.actions());
    }

    private static List<Transition> availableTransitions(PreparedPlan prepared, State state) {
        List<Transition> transitions = new ArrayList<>();
        if (state.forwardIndex() < prepared.forwardEvents().size()) {
            transitions.add(new Transition(prepared.forwardEvents().get(state.forwardIndex()).action(), -1));
        }
        for (int queueIndex = 0; queueIndex < prepared.recoveryQueues().size(); queueIndex++) {
            RecoveryQueue queue = prepared.recoveryQueues().get(queueIndex);
            int position = state.queuePositions().get(queueIndex);
            if (queue.releaseForwardCount() <= state.forwardIndex() && position < queue.actions().size()) {
                transitions.add(new Transition(queue.actions().get(position), queueIndex));
            }
        }
        return transitions;
    }

    private static State apply(State state, Transition transition) {
        if (transition.queueIndex() < 0) {
            return new State(state.forwardIndex() + 1, state.queuePositions());
        }
        List<Integer> positions = new ArrayList<>(state.queuePositions());
        positions.set(transition.queueIndex(), positions.get(transition.queueIndex()) + 1);
        return new State(state.forwardIndex(), positions);
    }

    private static boolean terminal(PreparedPlan prepared, State state) {
        if (state.forwardIndex() != prepared.forwardEvents().size()) {
            return false;
        }
        for (int index = 0; index < prepared.recoveryQueues().size(); index++) {
            if (state.queuePositions().get(index) != prepared.recoveryQueues().get(index).actions().size()) {
                return false;
            }
        }
        return true;
    }

    private enum Policy {
        EARLIEST,
        LATEST,
        ALTERNATING_RECOVERY_FIRST,
        ALTERNATING_FORWARD_FIRST
    }

    private record PreparedPlan(
            List<ForwardEvent> forwardEvents,
            List<RecoveryQueue> recoveryQueues,
            List<FaultSlotGenerationDiagnostic> faultSlotDiagnostics) {
    }

    private record ForwardEvent(FaultScenarioAction action, boolean successfulFinalBoundary) {
    }

    private record RecoveryQueue(String sagaInstanceId,
                                 int releaseForwardCount,
                                 List<FaultScenarioAction> actions) {
    }

    private record Transition(FaultScenarioAction action, int queueIndex) {
    }

    private record State(int forwardIndex, List<Integer> queuePositions) {
        private State {
            queuePositions = List.copyOf(queuePositions);
        }

        private static State initial(int queueCount) {
            return new State(0, Collections.nCopies(queueCount, 0));
        }
    }

    private static final class MutableState {
        private int forwardIndex;
        private final int[] queuePositions;

        private MutableState(int queueCount) {
            this.queuePositions = new int[queueCount];
        }

        private boolean terminal(PreparedPlan prepared) {
            if (forwardIndex != prepared.forwardEvents().size()) {
                return false;
            }
            for (int index = 0; index < prepared.recoveryQueues().size(); index++) {
                if (queuePositions[index] != prepared.recoveryQueues().get(index).actions().size()) {
                    return false;
                }
            }
            return true;
        }
    }

    private static final class ExactCounter {
        private final PreparedPlan prepared;
        private final Map<State, BigInteger> memoizedCounts = new HashMap<>();
        private long statesVisited;

        private ExactCounter(PreparedPlan prepared) {
            this.prepared = prepared;
        }

        private BigInteger count(State state) {
            BigInteger memoized = memoizedCounts.get(state);
            if (memoized != null) {
                return memoized;
            }
            statesVisited++;
            BigInteger count;
            if (terminal(prepared, state)) {
                count = BigInteger.ONE;
            } else {
                count = BigInteger.ZERO;
                for (Transition transition : availableTransitions(prepared, state)) {
                    count = count.add(count(apply(state, transition)));
                }
            }
            memoizedCounts.put(state, count);
            return count;
        }

        private long statesVisited() {
            return statesVisited;
        }
    }

    private static final class MutableMetrics {
        private long representativeCandidatesConstructed;
        private long materializedLeavesVisited;
    }
}
