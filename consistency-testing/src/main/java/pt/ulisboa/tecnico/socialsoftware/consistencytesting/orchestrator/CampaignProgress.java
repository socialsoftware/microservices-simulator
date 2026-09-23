package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.jspecify.annotations.Nullable;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.TestResult;

/**
 * Mutable campaign state used to produce durable, immutable report snapshots.
 */
final class CampaignProgress {

    /**
     * Version of the campaign metadata schema written to report JSON.
     * Increment it when a report reader can no longer safely interpret an older
     * report, for example after removing, renaming, changing the type of, or
     * changing the meaning of a field. Adding optional informational fields
     * does not require a version change.
     */
    static final int REPORT_SCHEMA_VERSION = 1;

    // TODO Record the source revision and whether the worktree was dirty, so a
    // report can be safely reused only with the code that produced it.

    private final String application;
    private final long masterSeed;
    private final List<String> springAppArgs;
    private final int iterationsPerGroup;
    private final String scheduleExplorationStrategy;
    private final String groupBudgetStrategy;
    private final String reportsDirectory;
    private final List<String> ignoredSemanticLockSelectors;
    private final List<String> groupSelectors;
    private final long startedAtEpochMillis;
    private final List<CatalogProgress> catalogs = new ArrayList<>();
    private final List<OrchestrationReport.Finding> findings = new ArrayList<>();
    private final CampaignMetrics outcomeMetrics;
    private @Nullable String lastCompletedGroupCatalog;
    private @Nullable String lastCompletedGroup;
    private @Nullable String planHash;
    private int plannedRunBudget;
    // Fixed iterations-per-group mode sets min=max=batch=iterationsPerGroup.
    private int minimumRunsPerGroup;
    private int maximumRunsPerGroup;
    private int allocationBatchSize;
    private final List<OrchestrationReport.BudgetAllocation> budgetAllocations = new ArrayList<>();

    CampaignProgress(
            String application,
            long masterSeed,
            List<String> springAppArgs,
            int iterationsPerGroup,
            String scheduleExplorationStrategy,
            String groupBudgetStrategy,
            String reportsDirectory,
            List<String> ignoredSemanticLockSelectors,
            List<String> groupSelectors,
            long startedAtEpochMillis) {

        this.application = application;
        this.masterSeed = masterSeed;
        this.springAppArgs = List.copyOf(springAppArgs);
        this.iterationsPerGroup = iterationsPerGroup;
        this.scheduleExplorationStrategy = scheduleExplorationStrategy;
        this.groupBudgetStrategy = groupBudgetStrategy;
        this.reportsDirectory = reportsDirectory;
        this.ignoredSemanticLockSelectors = List.copyOf(ignoredSemanticLockSelectors);
        this.groupSelectors = List.copyOf(groupSelectors);
        this.startedAtEpochMillis = startedAtEpochMillis;
        this.outcomeMetrics = new CampaignMetrics(startedAtEpochMillis);
        this.minimumRunsPerGroup = iterationsPerGroup;
        this.maximumRunsPerGroup = iterationsPerGroup;
        this.allocationBatchSize = iterationsPerGroup;
    }

    void setPlanHash(String planHash) {
        if (planHash == null || planHash.isBlank()) {
            throw new IllegalArgumentException("planHash cannot be blank");
        }
        this.planHash = planHash;
    }

    void registerCatalog(String name, int functionalitiesProfiled, int possiblePairs, int groupsPlanned) {
        catalogs.add(new CatalogProgress(name, functionalitiesProfiled, possiblePairs, groupsPlanned));
    }

    void configureGroupBudget(
            int plannedRunBudget,
            int minimumRunsPerGroup,
            int maximumRunsPerGroup,
            int allocationBatchSize) {

        this.plannedRunBudget = plannedRunBudget;
        this.minimumRunsPerGroup = minimumRunsPerGroup;
        this.maximumRunsPerGroup = maximumRunsPerGroup;
        this.allocationBatchSize = allocationBatchSize;
    }

    void recordBudgetAllocation(
            String phase,
            String catalog,
            String group,
            int requestedRuns,
            long durationMillis,
            double priorityScore,
            AdaptiveGroupBudgetAllocator.BatchFeedback feedback) {

        budgetAllocations.add(new OrchestrationReport.BudgetAllocation(
                budgetAllocations.size(), phase, catalog, group,
                requestedRuns, feedback.completedRuns(), durationMillis, priorityScore, feedback.reward(),
                feedback.newBehaviors(), feedback.runsAddingFeatures(), feedback.newFindingFamilies()));
    }

    void recordCompletedGroup(
            String catalogName,
            OrchestrationReport.GroupSummary group,
            List<OrchestrationReport.Finding> groupFindings) {

        CatalogProgress catalog = catalogs.stream()
                .filter(candidate -> candidate.name.equals(catalogName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown campaign catalog: " + catalogName));

        OrchestrationReport.GroupSummary previous = catalog.groups.put(group.label(), group);
        catalog.runsExecuted += group.runsExecuted() - (previous == null ? 0 : previous.runsExecuted());
        catalog.findingCount += group.findingCount() - (previous == null ? 0 : previous.findingCount());

        findings.addAll(groupFindings);
        lastCompletedGroupCatalog = catalogName;
        lastCompletedGroup = group.label();
    }

    void recordCompletedRun(TestResult result) {
        outcomeMetrics.record(result);
    }

    /**
     * Creates an immutable report snapshot. A null finish time means the
     * campaign is still running; in that case the current time is used only to
     * calculate the elapsed duration, while the report keeps its finish time
     * absent.
     */
    OrchestrationReport snapshot(OrchestrationReport.CampaignStatus status, Long finishedAtEpochMillis) {
        long endedAt = finishedAtEpochMillis == null ? System.currentTimeMillis() : finishedAtEpochMillis;
        List<OrchestrationReport.CatalogSummary> catalogSummaries = catalogs.stream()
                .map(CatalogProgress::snapshot)
                .toList();

        int fixedRunsPlanned = catalogSummaries.stream()
                .mapToInt(OrchestrationReport.CatalogSummary::groupsPlanned)
                .sum() * iterationsPerGroup;
        int runsPlanned = groupBudgetStrategy.equals(GroupBudgetStrategy.FIXED_PER_GROUP.propertyValue())
                ? fixedRunsPlanned
                : plannedRunBudget;

        return new OrchestrationReport(
                application,
                masterSeed,
                springAppArgs,
                iterationsPerGroup,
                scheduleExplorationStrategy,
                reportsDirectory,
                REPORT_SCHEMA_VERSION,
                ignoredSemanticLockSelectors,
                groupSelectors,
                planHash,
                status,
                startedAtEpochMillis,
                finishedAtEpochMillis,
                lastCompletedGroupCatalog,
                lastCompletedGroup,
                endedAt - startedAtEpochMillis,
                outcomeMetrics.snapshot(runsPlanned),
                catalogSummaries,
                List.copyOf(findings),
                new OrchestrationReport.GroupBudgetReport(
                        groupBudgetStrategy, runsPlanned, minimumRunsPerGroup,
                        maximumRunsPerGroup, allocationBatchSize,
                        List.copyOf(budgetAllocations)));
    }

    private static final class CatalogProgress {
        private final String name;
        private final int functionalitiesProfiled;
        private final int possiblePairs;
        private final int groupsPlanned;
        /** Stores the latest snapshot per group label; insertion order keeps groups in their first-seen report order. */
        private final LinkedHashMap<String, OrchestrationReport.GroupSummary> groups = new LinkedHashMap<>();
        private int runsExecuted;
        private int findingCount;

        private CatalogProgress(String name, int functionalitiesProfiled, int possiblePairs, int groupsPlanned) {
            this.name = name;
            this.functionalitiesProfiled = functionalitiesProfiled;
            this.possiblePairs = possiblePairs;
            this.groupsPlanned = groupsPlanned;
        }

        private OrchestrationReport.CatalogSummary snapshot() {
            return new OrchestrationReport.CatalogSummary(
                    name,
                    functionalitiesProfiled,
                    possiblePairs,
                    groupsPlanned,
                    groupsPlanned - groups.size(),
                    runsExecuted,
                    findingCount,
                    List.copyOf(groups.values()));
        }
    }
}
