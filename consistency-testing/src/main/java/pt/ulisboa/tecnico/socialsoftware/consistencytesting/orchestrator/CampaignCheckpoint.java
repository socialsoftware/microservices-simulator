package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

/** Coordinates serialized campaign-summary checkpoints and graceful cancellation. */
final class CampaignCheckpoint {

    private final CampaignProgress progress;
    private final CampaignSummaryWriter summaryWriter;
    private OrchestrationReport latestSummary;
    private boolean cancellationRequested;
    private Long cancelledAtEpochMillis;

    CampaignCheckpoint(CampaignProgress progress, CampaignSummaryWriter summaryWriter) {
        this.progress = progress;
        this.summaryWriter = summaryWriter;
        this.latestSummary = progress.snapshot(OrchestrationReport.CampaignStatus.RUNNING, null);
    }

    synchronized OrchestrationReport write(
            OrchestrationReport.CampaignStatus status, Long finishedAtEpochMillis) {

        OrchestrationReport.CampaignStatus effectiveStatus = cancellationRequested
                ? OrchestrationReport.CampaignStatus.CANCELLED
                : status;
        Long effectiveFinishedAt = cancellationRequested ? cancelledAtEpochMillis : finishedAtEpochMillis;
        latestSummary = progress.snapshot(effectiveStatus, effectiveFinishedAt);
        summaryWriter.write(latestSummary);
        return latestSummary;
    }

    synchronized void cancel() {
        if (latestSummary.status() != OrchestrationReport.CampaignStatus.RUNNING) {
            return;
        }
        cancellationRequested = true;
        cancelledAtEpochMillis = System.currentTimeMillis();
        write(OrchestrationReport.CampaignStatus.CANCELLED, cancelledAtEpochMillis);
    }
}
