package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * What a campaign found, and enough about how it ran to reproduce it.
 *
 * @param application               the Spring application class explored
 * @param masterSeed                the seed the whole campaign derived from;
 *                                  replaying
 *                                  it reproduces every schedule
 * @param springAppArgs             application arguments used for this campaign
 * @param iterationsPerGroup        oracle runs performed per planned group
 * @param reportsDirectory          where the per-run reports were written
 * @param status                    whether the campaign is still running, ended
 *                                  normally, was interrupted, or failed
 * @param startedAtEpochMillis      campaign start timestamp
 * @param finishedAtEpochMillis     campaign end timestamp, absent while running
 * @param lastCompletedGroupCatalog catalog containing the latest durable group
 *                                  checkpoint
 * @param lastCompletedGroup        latest group checkpoint, absent before group
 *                                  exploration
 * @param durationMillis            wall-clock duration of the campaign
 * @param catalogs                  per-catalog totals, in the order they were
 *                                  explored
 * @param findings                  every run worth attention, in the order
 *                                  found
 */
public record OrchestrationReport(
        String application,
        long masterSeed,
        List<String> springAppArgs,
        int iterationsPerGroup,
        String reportsDirectory,
        CampaignStatus status,
        long startedAtEpochMillis,
        Long finishedAtEpochMillis,
        String lastCompletedGroupCatalog,
        String lastCompletedGroup,
        long durationMillis,
        List<CatalogSummary> catalogs,
        List<Finding> findings) {

    public enum CampaignStatus {
        RUNNING,
        COMPLETED,
        CANCELLED,
        FAILED
    }

    /**
     * @param possiblePairs    how many pairs brute force would have run
     * @param groupsPlanned    how many pairs the planner actually scheduled
     * @param unexploredGroups groups planned but not explored (only non-zero
     *                         if a campaign stops early)
     * @param findingCount     how many runs the planner found worth
     *                         attention
     * @param groups           completed groups, in exploration order (by group
     *                         label, so completed portions of two campaigns line
     *                         up entry by entry)
     */
    public record CatalogSummary(
            String name,
            int functionalitiesProfiled,
            int possiblePairs,
            int groupsPlanned,
            int unexploredGroups,
            int runsExecuted,
            int findingCount,
            List<GroupSummary> groups) {
    }

    /**
     * @param conflictIdentities the aggregate handles (or types) the planner
     *                           paired these two functionalities on
     * @param findingCount       how many runs of this group the planner found worth
     *                           attention
     * @param anomalyCounts      anomaly type -> how many runs of this group
     *                           exhibited it
     */
    public record GroupSummary(
            String label,
            String first,
            String second,
            boolean selfPair,
            List<String> conflictIdentities,
            int runsExecuted,
            int findingCount,
            Map<String, Integer> anomalyCounts) {
    }

    /**
     * One run worth a developer's attention.
     *
     * @param runIndex   0-based index of the run within its group
     * @param reportFile the run's full report path, relative to the reports
     *                   directory
     */
    public record Finding(
            String catalog,
            String group,
            int runIndex,
            List<String> statuses,
            List<String> anomalyTypes,
            List<String> exceptionSteps,
            String reportFile) {
    }

    public boolean hasFindings() {
        return !findings.isEmpty();
    }

    /**
     * The findings that {@code accepted} does not match — the ones an application
     * has not declared expected. Use it to whitelist anomalies an application
     * plants on purpose (or wants to ignore):
     *
     * <pre>{@code
     * List<Finding> unexpected = report.findingsExcluding(
     *         finding -> finding.group().startsWith("joinTournamentA__joinTournamentB"));
     * }</pre>
     */
    public List<Finding> findingsExcluding(Predicate<Finding> accepted) {
        return findings.stream().filter(finding -> !accepted.test(finding)).toList();
    }

    /** Fails if the report has any findings. */
    public void assertNoFindings() {
        assertNoFindingsExcept(finding -> false); // accept nothing, so any finding is rejected
    }

    /** Fails unless every finding is matched by {@code accepted}. */
    public void assertNoFindingsExcept(Predicate<Finding> accepted) {
        List<Finding> unexpected = findingsExcluding(accepted);
        if (!unexpected.isEmpty()) {
            throw new AssertionError(
                    "%d unexpected finding(s) in %d run(s):%n%s%n%n%s".formatted(
                            unexpected.size(),
                            totalRuns(),
                            unexpected.stream().map(OrchestrationReport::describe)
                                    .collect(Collectors.joining(System.lineSeparator())),
                            summary()));
        }
    }

    public int totalRuns() {
        return catalogs.stream().mapToInt(CatalogSummary::runsExecuted).sum();
    }

    public int plannedGroups() {
        return catalogs.stream().mapToInt(CatalogSummary::groupsPlanned).sum();
    }

    public int completedGroups() {
        return catalogs.stream().mapToInt(catalog -> catalog.groups().size()).sum();
    }

    /** A short, human-readable summary of the campaign. */
    public String summary() {
        String header = "Consistency campaign over %s [status=%s, seed=%d, iterationsPerGroup=%d, duration=%s]"
                .formatted(application, status, masterSeed, iterationsPerGroup, formatDuration(durationMillis));

        String reports = "reports: " + reportsDirectory;

        String perCatalog = catalogs.stream()
                .map(catalog -> "  catalog '%s': %d functionalities, %d/%d pairs planned, %d runs, %d finding(s)"
                        .formatted(catalog.name(), catalog.functionalitiesProfiled(),
                                catalog.groupsPlanned(), catalog.possiblePairs(),
                                catalog.runsExecuted(), catalog.findingCount()))
                .collect(Collectors.joining(System.lineSeparator()));

        String total = "total: %d/%d group(s), %d finding(s) in %d run(s)".formatted(
                completedGroups(), plannedGroups(), findings.size(), totalRuns());

        return String.join(System.lineSeparator(), header, reports, perCatalog, total);
    }

    /**
     * Duration as {@code 1h07m12s} / {@code 6m42s} / {@code 12s}, dropping the
     * units it does not need.
     */
    private static String formatDuration(long durationMillis) {
        Duration duration = Duration.ofMillis(durationMillis);
        if (duration.toHours() > 0) {
            return "%dh%02dm%02ds".formatted(
                    duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
        }
        if (duration.toMinutes() > 0) {
            return "%dm%02ds".formatted(duration.toMinutes(), duration.toSecondsPart());
        }
        return "%ds".formatted(duration.toSeconds());
    }

    private static String describe(Finding finding) {
        return "  - [%s / %s] run %d: statuses=%s, anomalies=%s, exceptions=%s (%s)".formatted(
                finding.catalog(), finding.group(), finding.runIndex(),
                finding.statuses(), finding.anomalyTypes(), finding.exceptionSteps(),
                finding.reportFile());
    }
}
