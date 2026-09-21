package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

import java.util.List;

/**
 * Scheduler decisions made during one run.
 * <p>
 * Choices are positions in each dynamically materialized ready set, not step IDs.
 * They therefore remain applicable when event, abort, or compensation steps differ
 * between runs.
 */
public record ScheduleTrace(List<ScheduleDecision> decisions) {

    private static final ScheduleTrace EMPTY = new ScheduleTrace(List.of());

    public ScheduleTrace {
        decisions = List.copyOf(decisions);
    }

    public static ScheduleTrace empty() {
        return EMPTY;
    }

    public List<Integer> choicePrefix() {
        return decisions.stream().map(ScheduleDecision::selectedIndex).toList();
    }
}
