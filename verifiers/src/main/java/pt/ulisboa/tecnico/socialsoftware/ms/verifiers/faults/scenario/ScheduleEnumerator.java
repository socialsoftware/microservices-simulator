package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleEnumerator {

    private ScheduleEnumerator() {
    }

    public static Result enumerate(List<SagaScheduleInput> sagaInputs,
                                   ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                   int maxSchedulesPerInputTuple,
                                   long deterministicSeed) {
        List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates = List.of();
        if (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED) {
            List<SagaDefinition> sagas = (sagaInputs == null ? List.<SagaScheduleInput>of() : sagaInputs).stream()
                    .filter(Objects::nonNull)
                    .map(input -> new SagaDefinition(input.sagaFqn(), input.steps(), List.of()))
                    .toList();
            conflictCandidates = ConflictGraphBuilder.build(sagas, new ScenarioGeneratorConfig()).conflictCandidates();
        }
        return enumerate(sagaInputs, scheduleStrategy, maxSchedulesPerInputTuple, deterministicSeed, conflictCandidates);
    }

    public static Result enumerate(List<SagaScheduleInput> sagaInputs,
                                   ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                   int maxSchedulesPerInputTuple,
                                   long deterministicSeed,
                                   List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates) {
        List<SagaScheduleInput> orderedInputs = sagaInputs == null ? List.of() : sagaInputs.stream()
                .filter(Objects::nonNull)
                .sorted(Comparator
                        .comparing(SagaScheduleInput::sagaFqn, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(SagaScheduleInput::sagaInstanceId, Comparator.nullsFirst(String::compareTo)))
                .toList();

        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        LinkedHashSet<String> warnings = new LinkedHashSet<>();
        List<List<ScheduledStep>> schedules = new ArrayList<>();
        boolean[] capped = new boolean[1];

        int maxSchedules = Math.max(0, maxSchedulesPerInputTuple);
        if (maxSchedules == 0) {
            warnings.add("schedule cap disabled all schedules");
            counts.put("schedulesSeen", 0);
            counts.put("schedulesEmitted", 0);
            counts.put("schedulesCapped", 1);
            return new Result(List.of(), Collections.unmodifiableMap(counts), List.copyOf(warnings));
        }

        if (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING) {
            enumerateInterleavings(orderedInputs, maxSchedules, schedules, warnings, capped);
        } else if (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED) {
            enumerateSegmentCompressed(orderedInputs, conflictCandidates, maxSchedules, schedules, warnings, capped);
        } else {
            schedules.add(buildSerialSchedule(orderedInputs));
        }

        counts.put("schedulesSeen", schedules.size());
        counts.put("schedulesEmitted", schedules.size());
        counts.put("schedulesCapped", capped[0] ? 1 : 0);

        return new Result(List.copyOf(schedules), Collections.unmodifiableMap(counts), List.copyOf(warnings));
    }

    private static void enumerateSegmentCompressed(List<SagaScheduleInput> sagaInputs,
                                                   List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates,
                                                   int maxSchedules,
                                                   List<List<ScheduledStep>> schedules,
                                                   LinkedHashSet<String> warnings,
                                                   boolean[] capped) {
        List<List<List<StepDefinition>>> segmentsBySaga = sagaInputs.stream()
                .map(input -> anchorSegments(input.steps(), conflictAnchorIndexes(input, conflictCandidates)))
                .toList();
        if (segmentsBySaga.stream().mapToInt(List::size).sum() == 0) {
            schedules.add(buildSerialSchedule(sagaInputs));
            return;
        }
        enumerateSegmentInterleavings(sagaInputs, segmentsBySaga, maxSchedules, schedules, warnings, capped);
    }

    private static List<Integer> conflictAnchorIndexes(SagaScheduleInput sagaInput, List<ConflictGraphBuilder.ConflictCandidate> conflictCandidates) {
        LinkedHashSet<String> anchorStepIds = new LinkedHashSet<>();
        for (ConflictGraphBuilder.ConflictCandidate candidate : conflictCandidates == null ? List.<ConflictGraphBuilder.ConflictCandidate>of() : conflictCandidates) {
            if (Objects.equals(candidate.leftSagaFqn(), sagaInput.sagaFqn())) {
                anchorStepIds.add(candidate.leftStepId());
            }
            if (Objects.equals(candidate.rightSagaFqn(), sagaInput.sagaFqn())) {
                anchorStepIds.add(candidate.rightStepId());
            }
        }
        List<Integer> anchors = new ArrayList<>();
        for (int stepIndex = 0; stepIndex < sagaInput.steps().size(); stepIndex++) {
            StepDefinition step = sagaInput.steps().get(stepIndex);
            if (anchorStepIds.contains(step.deterministicId())) {
                anchors.add(stepIndex);
            }
        }
        return anchors;
    }

    private static List<List<StepDefinition>> anchorSegments(List<StepDefinition> steps, List<Integer> anchorIndexes) {
        List<List<StepDefinition>> segments = new ArrayList<>();
        int segmentStart = 0;
        for (Integer anchorIndex : anchorIndexes) {
            segments.add(List.copyOf(steps.subList(segmentStart, anchorIndex + 1)));
            segmentStart = anchorIndex + 1;
        }
        return List.copyOf(segments);
    }

    private static void enumerateSegmentInterleavings(List<SagaScheduleInput> sagaInputs,
                                                      List<List<List<StepDefinition>>> segmentsBySaga,
                                                      int maxSchedules,
                                                      List<List<ScheduledStep>> schedules,
                                                      LinkedHashSet<String> warnings,
                                                      boolean[] capped) {
        List<Integer> remaining = new ArrayList<>(Collections.nCopies(sagaInputs.size(), 0));
        List<SegmentToken> current = new ArrayList<>();
        int totalSegments = segmentsBySaga.stream().mapToInt(List::size).sum();
        backtrackSegments(sagaInputs, segmentsBySaga, remaining, current, schedules, maxSchedules, totalSegments, warnings, capped);
    }

    private static boolean backtrackSegments(List<SagaScheduleInput> sagaInputs,
                                             List<List<List<StepDefinition>>> segmentsBySaga,
                                             List<Integer> nextIndexes,
                                             List<SegmentToken> current,
                                             List<List<ScheduledStep>> schedules,
                                             int maxSchedules,
                                             int totalSegments,
                                             LinkedHashSet<String> warnings,
                                             boolean[] capped) {
        if (current.size() == totalSegments) {
            schedules.add(expandSegments(sagaInputs, segmentsBySaga, current));
            return true;
        }

        for (int sagaIndex = 0; sagaIndex < sagaInputs.size(); sagaIndex++) {
            int segmentIndex = nextIndexes.get(sagaIndex);
            if (segmentIndex >= segmentsBySaga.get(sagaIndex).size()) {
                continue;
            }
            nextIndexes.set(sagaIndex, segmentIndex + 1);
            current.add(new SegmentToken(sagaIndex, segmentIndex));
            boolean exhausted = backtrackSegments(sagaInputs, segmentsBySaga, nextIndexes, current, schedules, maxSchedules, totalSegments, warnings, capped);
            current.remove(current.size() - 1);
            nextIndexes.set(sagaIndex, segmentIndex);
            if (!exhausted) {
                return false;
            }
            if (schedules.size() >= maxSchedules && hasRemainingSegmentChoice(segmentsBySaga, nextIndexes, sagaIndex + 1)) {
                warnings.add("schedule cap reached at maxSchedulesPerInputTuple=" + maxSchedules);
                capped[0] = true;
                return false;
            }
        }
        return true;
    }

    private static boolean hasRemainingSegmentChoice(List<List<List<StepDefinition>>> segmentsBySaga, List<Integer> nextIndexes, int startIndex) {
        for (int index = Math.max(0, startIndex); index < segmentsBySaga.size(); index++) {
            if (nextIndexes.get(index) < segmentsBySaga.get(index).size()) {
                return true;
            }
        }
        return false;
    }

    private static List<ScheduledStep> expandSegments(List<SagaScheduleInput> sagaInputs,
                                                      List<List<List<StepDefinition>>> segmentsBySaga,
                                                      List<SegmentToken> tokens) {
        List<ScheduledStep> schedule = new ArrayList<>();
        for (SegmentToken token : tokens) {
            SagaScheduleInput sagaInput = sagaInputs.get(token.sagaIndex());
            for (StepDefinition step : segmentsBySaga.get(token.sagaIndex()).get(token.segmentIndex())) {
                addScheduledStep(schedule, sagaInput, step);
            }
        }
        appendSegmentCompressedTail(schedule, sagaInputs, segmentsBySaga);
        return List.copyOf(schedule);
    }

    private static void appendSegmentCompressedTail(List<ScheduledStep> schedule,
                                                    List<SagaScheduleInput> sagaInputs,
                                                    List<List<List<StepDefinition>>> segmentsBySaga) {
        for (int sagaIndex = 0; sagaIndex < sagaInputs.size(); sagaIndex++) {
            SagaScheduleInput sagaInput = sagaInputs.get(sagaIndex);
            int emittedSteps = segmentsBySaga.get(sagaIndex).stream().mapToInt(List::size).sum();
            for (int stepIndex = emittedSteps; stepIndex < sagaInput.steps().size(); stepIndex++) {
                addScheduledStep(schedule, sagaInput, sagaInput.steps().get(stepIndex));
            }
        }
    }

    private static void enumerateInterleavings(List<SagaScheduleInput> sagaInputs,
                                               int maxSchedules,
                                               List<List<ScheduledStep>> schedules,
                                               LinkedHashSet<String> warnings,
                                               boolean[] capped) {
        List<Integer> remaining = new ArrayList<>(Collections.nCopies(sagaInputs.size(), 0));
        List<ScheduledStep> current = new ArrayList<>();
        int totalSteps = sagaInputs.stream().mapToInt(input -> input.steps().size()).sum();
        backtrack(sagaInputs, remaining, current, schedules, maxSchedules, totalSteps, warnings, capped);
    }

    private static boolean backtrack(List<SagaScheduleInput> sagaInputs,
                                     List<Integer> nextIndexes,
                                     List<ScheduledStep> current,
                                     List<List<ScheduledStep>> schedules,
                                     int maxSchedules,
                                     int totalSteps,
                                     LinkedHashSet<String> warnings,
                                     boolean[] capped) {
        if (current.size() == totalSteps) {
            schedules.add(List.copyOf(current));
            return true;
        }

        for (int sagaIndex = 0; sagaIndex < sagaInputs.size(); sagaIndex++) {
            SagaScheduleInput sagaInput = sagaInputs.get(sagaIndex);
            int stepIndex = nextIndexes.get(sagaIndex);
            if (stepIndex >= sagaInput.steps().size()) {
                continue;
            }

            StepDefinition step = sagaInput.steps().get(stepIndex);
            int scheduleOrder = current.size();
            String scheduledStepId = ScenarioIdGenerator.scheduledStepId(sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder);
            ScheduledStep scheduledStep = new ScheduledStep(scheduledStepId, sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder, mergeWarnings(step.warnings()));

            nextIndexes.set(sagaIndex, stepIndex + 1);
            current.add(scheduledStep);
            boolean exhausted = backtrack(sagaInputs, nextIndexes, current, schedules, maxSchedules, totalSteps, warnings, capped);
            current.remove(current.size() - 1);
            nextIndexes.set(sagaIndex, stepIndex);

            if (!exhausted) {
                return false;
            }

            if (schedules.size() >= maxSchedules && hasRemainingChoice(sagaInputs, nextIndexes, sagaIndex + 1)) {
                warnings.add("schedule cap reached at maxSchedulesPerInputTuple=" + maxSchedules);
                capped[0] = true;
                return false;
            }
        }

        return true;
    }

    private static boolean hasRemainingChoice(List<SagaScheduleInput> sagaInputs, List<Integer> nextIndexes, int startIndex) {
        for (int index = Math.max(0, startIndex); index < sagaInputs.size(); index++) {
            if (nextIndexes.get(index) < sagaInputs.get(index).steps().size()) {
                return true;
            }
        }
        return false;
    }

    private static List<ScheduledStep> buildSerialSchedule(List<SagaScheduleInput> sagaInputs) {
        List<ScheduledStep> schedule = new ArrayList<>();
        for (SagaScheduleInput sagaInput : sagaInputs) {
            for (StepDefinition step : sagaInput.steps()) {
                addScheduledStep(schedule, sagaInput, step);
            }
        }
        return List.copyOf(schedule);
    }

    private static void addScheduledStep(List<ScheduledStep> schedule, SagaScheduleInput sagaInput, StepDefinition step) {
        int scheduleOrder = schedule.size();
        String scheduledStepId = ScenarioIdGenerator.scheduledStepId(sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder);
        schedule.add(new ScheduledStep(scheduledStepId, sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder, mergeWarnings(step.warnings())));
    }

    private static List<String> mergeWarnings(List<String> warnings) {
        LinkedHashSet<String> merged = new LinkedHashSet<>();
        if (warnings != null) {
            merged.addAll(warnings);
        }
        return List.copyOf(merged);
    }

    public record SagaScheduleInput(String sagaInstanceId, String sagaFqn, List<StepDefinition> steps) {

        public SagaScheduleInput {
            sagaInstanceId = sagaInstanceId == null || sagaInstanceId.isBlank() ? null : sagaInstanceId;
            sagaFqn = sagaFqn == null || sagaFqn.isBlank() ? null : sagaFqn;
            steps = steps == null ? List.of() : List.copyOf(steps);
        }
    }

    public record Result(List<List<ScheduledStep>> schedules, Map<String, Integer> counts, List<String> warnings) {
    }

    private record SegmentToken(int sagaIndex, int segmentIndex) {
    }
}
