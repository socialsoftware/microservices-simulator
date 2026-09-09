package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/** Computes isolated report-directory paths for experiments. */
final class ExperimentReportsDirectory {

    private static final DateTimeFormatter NAME_TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")
            .withZone(ZoneOffset.UTC);

    private ExperimentReportsDirectory() {
    }

    /** Returns the default isolated report path for one experiment run. */
    static Path pathForExperiment(Instant startedAt, UUID runId) {
        String timestamp = NAME_TIMESTAMP.format(startedAt);
        String suffix = runId.toString().substring(0, 8);
        return Path.of("target", "consistency-experiments", timestamp + "-" + suffix);
    }
}
