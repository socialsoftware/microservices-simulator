package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.util.ArrayList;
import java.util.List;

/**
 * Mutable campaign state used to produce durable, immutable report snapshots.
 */
final class CampaignProgress {

    private final String application;
    private final long masterSeed;
    private final List<String> springAppArgs;
    private final int iterationsPerGroup;
    private final String reportsDirectory;
    private final long startedAtEpochMillis;
    private final List<CatalogProgress> catalogs = new ArrayList<>();
    private final List<OrchestrationReport.Finding> findings = new ArrayList<>();
    private String lastCompletedGroupCatalog;
    private String lastCompletedGroup;

    CampaignProgress(
            String application,
            long masterSeed,
            List<String> springAppArgs,
            int iterationsPerGroup,
            String reportsDirectory,
            long startedAtEpochMillis) {

        this.application = application;
        this.masterSeed = masterSeed;
        this.springAppArgs = List.copyOf(springAppArgs);
        this.iterationsPerGroup = iterationsPerGroup;
        this.reportsDirectory = reportsDirectory;
        this.startedAtEpochMillis = startedAtEpochMillis;
    }

    void registerCatalog(String name, int functionalitiesProfiled, int possiblePairs, int groupsPlanned) {
        catalogs.add(new CatalogProgress(name, functionalitiesProfiled, possiblePairs, groupsPlanned));
    }

    void recordCompletedGroup(
            String catalogName,
            OrchestrationReport.GroupSummary group,
            List<OrchestrationReport.Finding> groupFindings) {

        CatalogProgress catalog = catalogs.stream()
                .filter(candidate -> candidate.name.equals(catalogName))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown campaign catalog: " + catalogName));
        catalog.groups.add(group);
        catalog.runsExecuted += group.runsExecuted();
        catalog.findingCount += group.findingCount();
        findings.addAll(groupFindings);
        lastCompletedGroupCatalog = catalogName;
        lastCompletedGroup = group.label();
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

        return new OrchestrationReport(
                application,
                masterSeed,
                springAppArgs,
                iterationsPerGroup,
                reportsDirectory,
                status,
                startedAtEpochMillis,
                finishedAtEpochMillis,
                lastCompletedGroupCatalog,
                lastCompletedGroup,
                endedAt - startedAtEpochMillis,
                catalogSummaries,
                List.copyOf(findings));
    }

    private static final class CatalogProgress {
        private final String name;
        private final int functionalitiesProfiled;
        private final int possiblePairs;
        private final int groupsPlanned;
        private final List<OrchestrationReport.GroupSummary> groups = new ArrayList<>();
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
                    List.copyOf(groups));
        }
    }
}
