package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralCoverage;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.ScheduleExplorationStrategy;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.utils.StringUtils;

/**
 * What a campaign found, and enough about how it ran to reproduce it.
 *
 * @param application                  the Spring application class explored
 * @param masterSeed                   the seed the whole campaign derived from;
 *                                     replaying it reproduces every schedule
 * @param springAppArgs                application arguments used for this
 *                                     campaign
 * @param iterationsPerGroup           oracle runs per group under fixed budgeting
 * @param scheduleExplorationStrategy  within-group scheduling policy
 * @param reportsDirectory             where the per-run reports were written
 * @param reportSchemaVersion          version of the campaign/report metadata
 *                                     schema
 * @param ignoredSemanticLockSelectors semantic-lock selectors omitted during
 *                                     exploration; empty for a normal campaign
 * @param groupSelectors               exact {@code catalog/group-label}
 *                                     selections; empty when every planned
 *                                     group
 *                                     is explored
 * @param planHash                     SHA-256 fingerprint of the plan explored;
 *                                     {@code null} until planning completes
 * @param status                       whether the campaign is still running,
 *                                     ended normally, was interrupted, or
 *                                     failed
 * @param startedAtEpochMillis         campaign start timestamp
 * @param finishedAtEpochMillis        campaign end timestamp, {@code null}
 *                                     while running
 * @param lastCompletedGroupCatalog    catalog containing the latest durable
 *                                     group checkpoint, or {@code null} before
 *                                     any group completes
 * @param lastCompletedGroup           latest group checkpoint, or {@code null}
 *                                     before exploration
 * @param durationMillis               wall-clock duration of the campaign
 * @param outcomeMetrics               counts and timings for signals observed
 *                                     in completed oracle runs
 * @param catalogs                     per-catalog totals, in the order they
 *                                     were explored
 * @param findings                     every run worth attention, in the order
 *                                     found
 * @param groupBudget                  campaign-level run allocation policy and
 *                                     auditable batch decisions
 */
public record OrchestrationReport(
        String application,
        long masterSeed,
        List<String> springAppArgs,
        int iterationsPerGroup,
        String scheduleExplorationStrategy,
        String reportsDirectory,
        int reportSchemaVersion,
        List<String> ignoredSemanticLockSelectors,
        List<String> groupSelectors,
        @Nullable String planHash,
        CampaignStatus status,
        long startedAtEpochMillis,
        @Nullable Long finishedAtEpochMillis,
        @Nullable String lastCompletedGroupCatalog,
        @Nullable String lastCompletedGroup,
        long durationMillis,
        OutcomeMetrics outcomeMetrics,
        List<CatalogSummary> catalogs,
        List<Finding> findings,
        @Nullable GroupBudgetReport groupBudget) {

    public OrchestrationReport {
        springAppArgs = List.copyOf(springAppArgs);
        scheduleExplorationStrategy = scheduleExplorationStrategy == null
                ? ScheduleExplorationStrategy.RANDOM_CONSTRAINTS.propertyValue()
                : scheduleExplorationStrategy;
        ignoredSemanticLockSelectors = List.copyOf(ignoredSemanticLockSelectors);
        groupSelectors = groupSelectors == null ? List.of() : List.copyOf(groupSelectors);
        groupBudget = groupBudget == null
                ? GroupBudgetReport.fixed(outcomeMetrics.runsPlanned(), iterationsPerGroup)
                : groupBudget;
    }

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
     * @param semanticLockGuardRejectionRuns            runs where every recorded
     *                                                  exception was caused by a
     *                                                  semantic-lock guard rejection,
     *                                                  with no status, anomaly, or
     *                                                  invariant violation; these
     *                                                  are evidence, not findings
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
            int semanticLockGuardRejectionRuns,
            Map<String, Integer> statusRunCounts) {

        public OutcomeMetrics {
            violatedInterInvariantNames = List.copyOf(violatedInterInvariantNames);
            // TreeMap so serialized reports keep status names in alphabetical order.
            statusRunCounts = Collections.unmodifiableMap(new TreeMap<>(statusRunCounts));
        }

    }

    /**
     * @param possiblePairs    how many pairs brute force would have run
     * @param groupsPlanned    how many planner-produced groups are in this
     *                         campaign's effective plan, after optional selection
     *                         is applied
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
     * @param conflictIdentities the aggregate handles (or types) on which the
     *                           members conflict; empty for an ALL_GROUPS pair
     *                           that the default policy would prune
     * @param findingCount       how many runs of this group the planner found worth
     *                           attention
     * @param anomalyCounts      anomaly type -> how many runs of this group
     *                           exhibited it
     * @param behavioralCoverage behavioral diversity summary for this group
     */
    public record GroupSummary(
            String label,
            String first,
            String second,
            boolean selfPair,
            List<String> conflictIdentities,
            int runsExecuted,
            int findingCount,
            Map<String, Integer> anomalyCounts,
            @Nullable BehavioralCoverage behavioralCoverage) {
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

    /** Campaign-level budget configuration and completed allocation history. */
    public record GroupBudgetReport(
            String strategy,
            /** Campaign-wide planned run count (fixed: groups × iterations; adaptive: configured total). */
            int plannedRunBudget,
            int minimumRunsPerGroup,
            int maximumRunsPerGroup,
            int batchSize,
            List<BudgetAllocation> allocations) {

        public GroupBudgetReport {
            Objects.requireNonNull(strategy);
            allocations = List.copyOf(allocations);
            if (plannedRunBudget < 0 || minimumRunsPerGroup < 1
                    || maximumRunsPerGroup < minimumRunsPerGroup || batchSize < 1) {
                throw new IllegalArgumentException("Invalid group budget report configuration");
            }
            for (int index = 0; index < allocations.size(); index++) {
                if (allocations.get(index).sequence() != index) {
                    throw new IllegalArgumentException("Budget allocation sequence must be contiguous from zero");
                }
            }
            int completedRuns = allocations.stream()
                    .mapToInt(BudgetAllocation::completedRuns).sum();
            if (completedRuns > plannedRunBudget) {
                throw new IllegalArgumentException("Budget allocations exceed planned run budget");
            }
        }

        static GroupBudgetReport fixed(int plannedRunBudget, int iterationsPerGroup) {
            // Fixed mode has min=max=batch=iterations and has no adaptive allocation history.
            return new GroupBudgetReport(
                    GroupBudgetStrategy.FIXED_PER_GROUP.propertyValue(),
                    plannedRunBudget, iterationsPerGroup, iterationsPerGroup,
                    iterationsPerGroup, List.of());
        }
    }

    /** Evidence produced by one completed group-budget allocation. */
    public record BudgetAllocation(
            /** Zero-based position in this campaign's allocation history. */
            int sequence,
            String phase,
            String catalog,
            String group,
            int requestedRuns,
            int completedRuns,
            long durationMillis,
            /** Group priority calculated immediately before this batch was run. */
            double priorityScore,
            /** Novelty reward calculated from this batch after it completed. */
            double reward,
            int newBehaviors,
            int runsAddingFeatures,
            int newFindingFamilies) {

        public BudgetAllocation {
            if (sequence < 0 || requestedRuns < 1 || completedRuns < 1
                    || completedRuns > requestedRuns || durationMillis < 0
                    || !Double.isFinite(priorityScore) || !Double.isFinite(reward)
                    || reward < 0.0 || newBehaviors < 0 || newBehaviors > completedRuns
                    || runsAddingFeatures < 0 || runsAddingFeatures > completedRuns
                    || newFindingFamilies < 0 || newFindingFamilies > completedRuns) {
                throw new IllegalArgumentException("Invalid group budget allocation evidence");
            }
        }
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

    public int uniqueGroupLocalBehaviors() {
        return catalogs.stream()
                .flatMap(catalog -> catalog.groups().stream())
                .filter(group -> group.behavioralCoverage() != null)
                .mapToInt(group -> group.behavioralCoverage().uniqueBehaviors())
                .sum();
    }

    public int duplicateBehaviorRuns() {
        return catalogs.stream()
                .flatMap(catalog -> catalog.groups().stream())
                .filter(group -> group.behavioralCoverage() != null)
                .mapToInt(group -> group.behavioralCoverage().duplicateRuns())
                .sum();
    }

    /** A short, human-readable summary of the campaign. */
    public String summary() {
        String configuredBudget = groupBudget.strategy().equals(GroupBudgetStrategy.FIXED_PER_GROUP.propertyValue())
                ? "iterationsPerGroup=" + iterationsPerGroup
                : "totalRunBudget=" + groupBudget.plannedRunBudget();
        String header = "Consistency campaign over %s [status=%s, seed=%d, %s, strategy=%s, duration=%s]"
                .formatted(application, status, masterSeed, configuredBudget,
                        scheduleExplorationStrategy, StringUtils.formatDuration(durationMillis));

        String reports = "reports: " + reportsDirectory;
        // Fixed mode reports min=max=batch=iterationsPerGroup; adaptive mode reports its configured range/batch.
        String budget = "group budget: %s, %d planned run(s), range %d..%d per group, batch %d"
                .formatted(groupBudget.strategy(), groupBudget.plannedRunBudget(),
                        groupBudget.minimumRunsPerGroup(), groupBudget.maximumRunsPerGroup(),
                        groupBudget.batchSize());
        String groupsSelected = groupSelectors.isEmpty()
                ? "groups: all planned groups"
                : "groups: " + groupSelectors;

        String perCatalog = catalogs.stream()
                .map(catalog -> "  catalog '%s': %d functionalities, %s, %d runs, %d finding(s)"
                        .formatted(catalog.name(), catalog.functionalitiesProfiled(),
                                describePlanSize(catalog), catalog.runsExecuted(), catalog.findingCount()))
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
        String semanticLockGuardRejections = "semantic-lock guard rejection runs: %d (not findings)"
                .formatted(outcomeMetrics.semanticLockGuardRejectionRuns());
        String statuses = "number of runs carrying each status: " + outcomeMetrics.statusRunCounts();
        boolean completeBehavioralCoverage = catalogs.stream()
                .flatMap(catalog -> catalog.groups().stream())
                .allMatch(group -> group.behavioralCoverage() != null);
        String behavior = completeBehavioralCoverage
                ? "behavioral coverage: %d unique group-local behavior(s), %d duplicate run(s), %.1f%% discovery"
                        .formatted(uniqueGroupLocalBehaviors(), duplicateBehaviorRuns(),
                                totalRuns() == 0 ? 0.0 : 100.0 * uniqueGroupLocalBehaviors() / totalRuns())
                : "behavioral coverage: unavailable in one or more group reports";

        return String.join(
                System.lineSeparator(), header, reports, groupsSelected, budget,
                perCatalog, total, outcomes, semanticLockGuardRejections, behavior, statuses);
    }

    private static String formatOptionalDuration(Long durationMillis) {
        return durationMillis == null ? "not observed" : StringUtils.formatDuration(durationMillis);
    }

    private String describePlanSize(CatalogSummary catalog) {
        if (groupSelectors.isEmpty()) {
            return "%d/%d pairs planned".formatted(catalog.groupsPlanned(), catalog.possiblePairs());
        }
        return "%d group(s) custom selected from %d possible pairs"
                .formatted(catalog.groupsPlanned(), catalog.possiblePairs());
    }

    private static String describe(Finding finding) {
        return "  - [%s / %s] run %d: statuses=%s, anomalies=%s, exceptions=%s (%s)".formatted(
                finding.catalog(), finding.group(), finding.runIndex(),
                finding.statuses(), finding.anomalyTypes(), finding.exceptionSteps(),
                finding.reportFile());
    }
}
