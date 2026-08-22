package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

class CampaignCheckpointTest {

    @Test
    void cancellationCannotBeOverwrittenByALaterRunningOrFailureCheckpoint(@TempDir Path tempDir) throws Exception {
        CampaignProgress progress = new CampaignProgress(
                "example.Application", 42L, List.of(), 20, "target/reports", 1_000L);
        CampaignSummaryWriter writer = new CampaignSummaryWriter(tempDir);
        CampaignCheckpoint checkpoint = new CampaignCheckpoint(progress, writer);

        checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);
        checkpoint.cancel();
        checkpoint.write(OrchestrationReport.CampaignStatus.RUNNING, null);
        OrchestrationReport finalSnapshot = checkpoint.write(
                OrchestrationReport.CampaignStatus.FAILED, System.currentTimeMillis());

        assertEquals(OrchestrationReport.CampaignStatus.CANCELLED, finalSnapshot.status());
        OrchestrationReport onDisk = new ObjectMapper().readValue(
                tempDir.resolve(CampaignSummaryWriter.FILE_NAME).toFile(), OrchestrationReport.class);
        assertEquals(OrchestrationReport.CampaignStatus.CANCELLED, onDisk.status());
    }
}
