package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class CampaignSummaryWriterTest {

    @Test
    void replacesThePreviousCheckpointWithAReadableSnapshot(@TempDir Path tempDir) throws Exception {
        Path reportsDirectory = tempDir.resolve("reports");
        CampaignSummaryWriter writer = new CampaignSummaryWriter(reportsDirectory);
        OrchestrationReport running = report(OrchestrationReport.CampaignStatus.RUNNING, null, 200L);
        OrchestrationReport completed = report(OrchestrationReport.CampaignStatus.COMPLETED, 300L, 300L);

        writer.write(running);
        Path summary = reportsDirectory.resolve(CampaignSummaryWriter.FILE_NAME);
        assertTrue(Files.exists(summary));
        assertEquals(running, new ObjectMapper().readValue(summary.toFile(), OrchestrationReport.class));

        writer.write(completed);

        assertEquals(completed, new ObjectMapper().readValue(summary.toFile(), OrchestrationReport.class));
        assertFalse(Files.exists(summary.resolveSibling(summary.getFileName() + ".tmp")));
    }

    private static OrchestrationReport report(
            OrchestrationReport.CampaignStatus status, Long finishedAtEpochMillis, long durationMillis) {

        return new OrchestrationReport(
                "example.Application", 42L, List.of("--example=true"), 20, "target/reports", status,
                100L, finishedAtEpochMillis, null, null, durationMillis, List.of(), List.of());
    }
}
