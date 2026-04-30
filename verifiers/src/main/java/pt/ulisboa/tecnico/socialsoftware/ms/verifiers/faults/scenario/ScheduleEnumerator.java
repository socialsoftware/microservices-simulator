package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScheduledStep;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.StepDefinition;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ScheduleEnumerator {

    private static final int SMALL_INTERLEAVING_STEP_BOUND = 12;

    private ScheduleEnumerator() {
    }

    public static Result enumerate(List<SagaScheduleInput> sagaInputs,
                                   ScenarioGeneratorConfig.ScheduleStrategy scheduleStrategy,
                                   int maxSchedulesPerInputTuple,
                                   long deterministicSeed) {
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

        List<StepDefinition> flattenedSteps = orderedInputs.stream().flatMap(input -> input.steps().stream()).toList();
        boolean useInterleaving = scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.ORDER_PRESERVING_INTERLEAVING
                || (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED
                && flattenedSteps.size() <= SMALL_INTERLEAVING_STEP_BOUND);

        if (scheduleStrategy == ScenarioGeneratorConfig.ScheduleStrategy.SEGMENT_COMPRESSED && !useInterleaving) {
            warnings.add("segment-compressed schedule strategy fell back to SERIAL for large tuples");
        }

        if (useInterleaving) {
            enumerateInterleavings(orderedInputs, maxSchedules, schedules, warnings, capped);
        } else {
            schedules.add(buildSerialSchedule(orderedInputs));
        }

        counts.put("schedulesSeen", schedules.size());
        counts.put("schedulesEmitted", schedules.size());
        counts.put("schedulesCapped", capped[0] ? 1 : 0);

        return new Result(List.copyOf(schedules), Collections.unmodifiableMap(counts), List.copyOf(warnings));
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
        int scheduleOrder = 0;
        for (SagaScheduleInput sagaInput : sagaInputs) {
            for (StepDefinition step : sagaInput.steps()) {
                String scheduledStepId = ScenarioIdGenerator.scheduledStepId(sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder);
                schedule.add(new ScheduledStep(scheduledStepId, sagaInput.sagaInstanceId(), step.deterministicId(), scheduleOrder, mergeWarnings(step.warnings())));
                scheduleOrder++;
            }
        }
        return List.copyOf(schedule);
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
}
