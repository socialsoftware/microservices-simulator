package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Computes a collision-free report directory for one campaign. */
final class CampaignReportsDirectory {

    private static final DateTimeFormatter NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneOffset.UTC);

    private CampaignReportsDirectory() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    static Path pathForCampaign(Path reportsRoot, Instant startedAt, UUID campaignId) {
        String timestamp = NAME_TIMESTAMP.format(startedAt);
        String suffix = campaignId.toString().substring(0, 8);
        return reportsRoot.resolve(timestamp + "-" + suffix);
    }
}
