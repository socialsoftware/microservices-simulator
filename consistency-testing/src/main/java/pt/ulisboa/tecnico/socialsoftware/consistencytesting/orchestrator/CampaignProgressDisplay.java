package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Locale;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

/** Formats a durable campaign checkpoint as one concise progress line. */
final class CampaignProgressDisplay {

    private CampaignProgressDisplay() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static String format(OrchestrationReport checkpoint) {
        OrchestrationReport.OutcomeMetrics outcomes = checkpoint.outcomeMetrics();
        int completedRuns = outcomes.runsCompleted();
        int plannedRuns = outcomes.runsPlanned();
        String percentage = String.format(Locale.ROOT, "%.1f", percentage(completedRuns, plannedRuns));

        return "checkpointed progress: %d/%d run(s) (%s%%), last completed group: %s, elapsed %s, rough ETA %s"
                .formatted(
                        completedRuns,
                        plannedRuns,
                        percentage,
                        lastCompletedGroup(checkpoint),
                        StringUtils.formatDuration(checkpoint.durationMillis()),
                        estimatedRemaining(checkpoint.durationMillis(), completedRuns, plannedRuns));
    }

    /**
     * Returns the percentage of completed runs relative to the planned runs.
     * If {@code plannedRuns} is 0, returns 0 as the percentage cannot be reliably
     * tracked.
     */
    private static double percentage(int completedRuns, int plannedRuns) {
        return plannedRuns == 0 ? 0 : 100.0 * completedRuns / plannedRuns;
    }

    private static String lastCompletedGroup(OrchestrationReport checkpoint) {
        if (checkpoint.lastCompletedGroup() == null) {
            return "not started";
        }
        return "catalog '%s', group '%s'".formatted(
                checkpoint.lastCompletedGroupCatalog(), checkpoint.lastCompletedGroup());
    }

    private static String estimatedRemaining(long elapsedMillis, int completedRuns, int plannedRuns) {
        if (completedRuns == 0 || completedRuns >= plannedRuns) {
            return "unavailable";
        }

        long remainingMillis = (long) Math.ceil(
                (double) elapsedMillis / completedRuns * (plannedRuns - completedRuns));
        return StringUtils.formatDuration(remainingMillis);
    }
}
