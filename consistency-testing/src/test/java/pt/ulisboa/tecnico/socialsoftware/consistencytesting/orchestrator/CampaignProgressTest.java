package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CampaignProgressTest {

    @Test
    void snapshotsKeepCompletedGroupsWhileLeavingTheRemainingPlanVisible() {
        CampaignProgress progress = new CampaignProgress(
                "example.Application", 42L, List.of("--example=true"), 20, "target/reports", List.of(), 1_000L);

        progress.registerCatalog("catalog", 3, 6, 2);
        OrchestrationReport beforeExploration = progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null);

        assertEquals(2, beforeExploration.plannedGroups());
        assertEquals(0, beforeExploration.completedGroups());
        assertEquals(2, beforeExploration.catalogs().getFirst().unexploredGroups());
        assertNull(beforeExploration.finishedAtEpochMillis());
        assertNull(beforeExploration.lastCompletedGroup());

        progress.setPlanHash("abc123");
        assertEquals("abc123", progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null).planHash());

        OrchestrationReport.GroupSummary completedGroup = new OrchestrationReport.GroupSummary(
                "first__second", "first", "second", false, List.of("Course"), 20, 1,
                Map.of("DIRTY_READ", 2));
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
}
