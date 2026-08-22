package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

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
 * @param outcomeMetrics            counts and timings for signals observed in
 *                                  completed oracle runs
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
        OutcomeMetrics outcomeMetrics,
        List<CatalogSummary> catalogs,
        List<Finding> findings) {

    public enum CampaignStatus {
        RUNNING,
        COMPLETED,
        CANCELLED,
        FAILED
    }

    /**
     * Metrics collected from completed oracle runs. They complement the
     * {@link CatalogSummary}s.
     *
     * @param runsPlanned                               planned runs in the catalogs
     *                                                  known at this checkpoint
     * @param runsCompleted                             runs that completed
     *                                                  execution so far
     * @param anomaliesObserved                         total anomaly instances
     * @param runsWithAnomalies                         runs containing an anomaly
     * @param firstAnomalyElapsedMillis                 elapsed time until the first
     *                                                  anomaly; {@code null} when
     *                                                  no anomaly was observed
     * @param interInvariantViolationsObserved          total inter-invariant
     *                                                  violation instances
     * @param runsWithInterInvariantViolations          runs containing an
     *                                                  inter-invariant violation
     * @param firstInterInvariantViolationElapsedMillis elapsed time until the
     *                                                  first inter-invariant
     *                                                  violation;
     *                                                  {@code null} when none was
     *                                                  observed
     * @param violatedInterInvariantNames               distinct invariant names
     *                                                  reported as broken
     * @param stepExceptionsObserved                    total failed step executions
     * @param runsWithStepExceptions                    runs with a failed step
     * @param statusRunCounts                           exact oracle status -> count
     *                                                  of completed runs carrying
     *                                                  it
     */
    public record OutcomeMetrics(
            int runsPlanned,
            int runsCompleted,
            int anomaliesObserved,
            int runsWithAnomalies,
            @Nullable Long firstAnomalyElapsedMillis,
            int interInvariantViolationsObserved,
            int runsWithInterInvariantViolations,
            @Nullable Long firstInterInvariantViolationElapsedMillis,
            List<String> violatedInterInvariantNames,
            int stepExceptionsObserved,
            int runsWithStepExceptions,
            Map<String, Integer> statusRunCounts) {

        public OutcomeMetrics {
            violatedInterInvariantNames = List.copyOf(violatedInterInvariantNames);
            // TreeMap so serialized reports keep status names in alphabetical order.
            statusRunCounts = Collections.unmodifiableMap(new TreeMap<>(statusRunCounts));
        }
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
                .formatted(application, status, masterSeed, iterationsPerGroup,
                        StringUtils.formatDuration(durationMillis));

        String reports = "reports: " + reportsDirectory;

        String perCatalog = catalogs.stream()
                .map(catalog -> "  catalog '%s': %d functionalities, %d/%d pairs planned, %d runs, %d finding(s)"
                        .formatted(catalog.name(), catalog.functionalitiesProfiled(),
                                catalog.groupsPlanned(), catalog.possiblePairs(),
                                catalog.runsExecuted(), catalog.findingCount()))
                .collect(Collectors.joining(System.lineSeparator()));

        String total = "total: %d/%d group(s), %d finding(s) in %d run(s)".formatted(
                completedGroups(), plannedGroups(), findings.size(), totalRuns());

        String outcomes = "outcomes: %d/%d run(s), %d anomaly instance(s) in %d run(s) (first %s), "
                + "%d inter-invariant violation(s) in %d run(s) (first %s), %d step exception(s) in %d run(s)";
        outcomes = outcomes.formatted(
                outcomeMetrics.runsCompleted(), outcomeMetrics.runsPlanned(),
                outcomeMetrics.anomaliesObserved(), outcomeMetrics.runsWithAnomalies(),
                formatOptionalDuration(outcomeMetrics.firstAnomalyElapsedMillis()),
                outcomeMetrics.interInvariantViolationsObserved(),
                outcomeMetrics.runsWithInterInvariantViolations(),
                formatOptionalDuration(outcomeMetrics.firstInterInvariantViolationElapsedMillis()),
                outcomeMetrics.stepExceptionsObserved(), outcomeMetrics.runsWithStepExceptions());
        String statuses = "number of runs carrying each status: " + outcomeMetrics.statusRunCounts();

        return String.join(System.lineSeparator(), header, reports, perCatalog, total, outcomes, statuses);
    }

    private static String formatOptionalDuration(Long durationMillis) {
        return durationMillis == null ? "not observed" : StringUtils.formatDuration(durationMillis);
    }

    private static String describe(Finding finding) {
        return "  - [%s / %s] run %d: statuses=%s, anomalies=%s, exceptions=%s (%s)".formatted(
                finding.catalog(), finding.group(), finding.runIndex(),
                finding.statuses(), finding.anomalyTypes(), finding.exceptionSteps(),
                finding.reportFile());
    }
}
