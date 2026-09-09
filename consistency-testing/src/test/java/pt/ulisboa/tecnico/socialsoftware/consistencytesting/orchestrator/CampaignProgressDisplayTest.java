package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

class CampaignProgressDisplayTest {

    @Test
    void formatsCompletedRunsPercentageGroupElapsedTimeAndRoughEta() {
        OrchestrationReport checkpoint = report(170, 1_070, 170_000L, "catalog", "first__second");

        assertEquals(
                "checkpointed progress: 170/1070 run(s) (15.9%), last completed group: "
                        + "catalog 'catalog', group 'first__second', elapsed 2m50s, rough ETA 15m00s",
                CampaignProgressDisplay.format(checkpoint));
    }

    @Test
    void leavesEtaUnavailableUntilAtLeastOneRunCompletes() {
        OrchestrationReport checkpoint = report(0, 20, 5_000L, null, null);

        assertEquals(
                "checkpointed progress: 0/20 run(s) (0.0%), last completed group: not started, "
                        + "elapsed 5s, rough ETA unavailable",
                CampaignProgressDisplay.format(checkpoint));
    }

    private static OrchestrationReport report(
            int runsCompleted,
            int runsPlanned,
            long durationMillis,
            String lastCompletedGroupCatalog,
            String lastCompletedGroup) {

        return new OrchestrationReport(
                "example.Application", 42L, List.of(), 20, "target/reports",
                1, List.of(), "abc123",
                OrchestrationReport.CampaignStatus.RUNNING,
                1_000L, null, lastCompletedGroupCatalog, lastCompletedGroup, durationMillis,
                new OrchestrationReport.OutcomeMetrics(
                        runsPlanned, runsCompleted, 0, 0, null, 0, 0, null, List.of(), 0, 0, Map.of()),
                List.of(), List.of());
    }
}
