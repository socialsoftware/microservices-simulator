package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.BehavioralCoverage;

class CampaignProgressTest {

    @Test
    void snapshotsKeepCompletedGroupsWhileLeavingTheRemainingPlanVisible() {
        CampaignProgress progress = new CampaignProgress(
                "example.Application", 42L, List.of("--example=true"), 20, "random-constraints",
                GroupBudgetStrategy.FIXED_PER_GROUP.propertyValue(), "target/reports",
                List.of(), List.of("catalog/first__second"), 1_000L);

        progress.registerCatalog("catalog", 3, 6, 2);
        OrchestrationReport beforeExploration = progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null);

        assertEquals(2, beforeExploration.plannedGroups());
        assertEquals(0, beforeExploration.completedGroups());
        assertEquals(2, beforeExploration.catalogs().getFirst().unexploredGroups());
        assertEquals(List.of("catalog/first__second"), beforeExploration.groupSelectors());
        assertNull(beforeExploration.finishedAtEpochMillis());
        assertNull(beforeExploration.lastCompletedGroup());

        progress.setPlanHash("abc123");
        assertEquals("abc123", progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null).planHash());

        OrchestrationReport.GroupSummary completedGroup = new OrchestrationReport.GroupSummary(
                "first__second", "first", "second", false, List.of("Course"), 20, 1,
                Map.of("DIRTY_READ", 2),
                new BehavioralCoverage("normalized-behavior-v3", 20, 4, 16, 0.2,
                        7, 3, 0.15,
                        IntStream.rangeClosed(1, 20).map(i -> Math.min(i, 4)).boxed().toList(),
                        IntStream.rangeClosed(1, 20).map(i -> i == 1 ? 3 : i == 2 ? 5 : 7).boxed().toList(),
                        IntStream.rangeClosed(1, 20).map(i -> i == 1 ? 3 : i <= 3 ? 2 : 0).boxed().toList()));
        OrchestrationReport.Finding finding = new OrchestrationReport.Finding(
                "catalog", "first__second", 7, List.of("CRITICAL_STEP_FAILURE"),
                List.of("DIRTY_READ"), List.of("first::step"), "catalog/first__second/test-report-00008.json");
        progress.recordCompletedGroup("catalog", completedGroup, List.of(finding));

        OrchestrationReport partial = progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null);

        assertEquals(1, partial.completedGroups());
        assertEquals(1, partial.catalogs().getFirst().unexploredGroups());
        assertEquals(20, partial.totalRuns());
        assertEquals(List.of(finding), partial.findings());
        assertEquals("catalog", partial.lastCompletedGroupCatalog());
        assertEquals("first__second", partial.lastCompletedGroup());

        OrchestrationReport completed = progress.snapshot(OrchestrationReport.CampaignStatus.COMPLETED, 2_500L);
        assertEquals(OrchestrationReport.CampaignStatus.COMPLETED, completed.status());
        assertEquals(2_500L, completed.finishedAtEpochMillis());
        assertEquals(1_500L, completed.durationMillis());
    }

    @Test
    void adaptiveSnapshotsReplacePartialGroupProgressWithoutDoubleCountingRuns() {
        CampaignProgress progress = new CampaignProgress(
                "example.Application", 42L, List.of(), 20, "feedback-guided",
                GroupBudgetStrategy.ADAPTIVE_NOVELTY.propertyValue(), "target/reports",
                List.of(), List.of(), 1_000L);
        progress.registerCatalog("catalog", 2, 3, 1);
        progress.configureGroupBudget(6, 2, 6, 2);

        // Record the warm-up allocation and initial snapshot for this group.
        AdaptiveGroupBudgetAllocator.BatchFeedback warmup =
                new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 2, 2, 0);
        progress.recordBudgetAllocation("WARMUP", "catalog", "first__second", 2, 100L, 0.0, warmup);
        progress.recordCompletedGroup("catalog", groupSummary(2, 0), List.of());

        // A later adaptive batch extends the same group snapshot and adds a finding at run four.
        AdaptiveGroupBudgetAllocator.BatchFeedback adaptive =
                new AdaptiveGroupBudgetAllocator.BatchFeedback(2, 1, 1, 1);
        progress.recordBudgetAllocation("ADAPTIVE", "catalog", "first__second", 2, 80L, 2.5, adaptive);
        progress.recordCompletedGroup("catalog", groupSummary(4, 1), List.of(
                new OrchestrationReport.Finding(
                        "catalog", "first__second", 3, List.of("INTER_INVARIANT_VIOLATION"),
                        List.of(), List.of(), "catalog/first__second/test-report-00004.json")));

        OrchestrationReport report = progress.snapshot(
                OrchestrationReport.CampaignStatus.RUNNING, null);

        assertEquals(4, report.totalRuns());
        assertEquals(1, report.findings().size());
        assertEquals(6, report.outcomeMetrics().runsPlanned());
        assertEquals(GroupBudgetStrategy.ADAPTIVE_NOVELTY.propertyValue(), report.groupBudget().strategy());
        assertEquals(2, report.groupBudget().allocations().size());
        assertEquals(1, report.catalogs().getFirst().groups().size());
    }

    @Test
    void unrestrictedAllGroupsTotalsAndEmptyConflictClassificationSurviveSnapshots() {
        CampaignProgress progress = new CampaignProgress(
                "example.Application", 42L, List.of(), 20, "target/reports", List.of(), List.of(), 1_000L);
        progress.registerCatalog("catalog", 2, 3, 3);
        progress.recordCompletedGroup("catalog", new OrchestrationReport.GroupSummary(
                "first__first", "first", "first", true, List.of(), 20, 0, Map.of(), null), List.of());

        OrchestrationReport report = progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null);

        assertEquals(report.catalogs().getFirst().possiblePairs(), report.catalogs().getFirst().groupsPlanned());
        assertEquals(List.of(), report.catalogs().getFirst().groups().getFirst().conflictIdentities());
        assertEquals(2, report.catalogs().getFirst().unexploredGroups());
    }

    private static OrchestrationReport.GroupSummary groupSummary(int runs, int findings) {
        return new OrchestrationReport.GroupSummary(
                "first__second", "first", "second", false, List.of("Course"),
                runs, findings, Map.of(),
                new BehavioralCoverage(
                        "normalized-behavior-v3", runs, runs, 0, 1.0,
                        runs, runs, 1.0,
                        IntStream.rangeClosed(1, runs).boxed().toList(),
                        IntStream.rangeClosed(1, runs).boxed().toList(),
                        Collections.nCopies(runs, 1)));
    }
}
