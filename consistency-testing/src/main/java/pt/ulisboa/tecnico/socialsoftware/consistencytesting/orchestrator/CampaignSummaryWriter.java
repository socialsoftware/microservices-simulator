package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Writes the campaign summary, replacing the previous summary when present.
 * <p>
 * The replacement is atomic when the filesystem supports {@code ATOMIC_MOVE};
 * otherwise, the writer uses a best-effort non-atomic replacement.
 */
final class CampaignSummaryWriter {

    static final String FILE_NAME = "campaign-summary.json";

    private final Path target;
    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    CampaignSummaryWriter(Path reportsDirectory) {
        this.target = reportsDirectory.resolve(FILE_NAME);
        try {
            Files.createDirectories(reportsDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create reports directory: " + reportsDirectory, e);
        }
    }

    /**
     * Writes the report as the campaign summary, replacing the previous summary
     * when present.
     * <p>
     * The report is serialized to a temporary file before replacement. Atomic
     * replacement is requested when supported by the filesystem; otherwise, the
     * fallback may be observed non-atomically.
     */
    void write(OrchestrationReport report) {
        Path temporary = target.resolveSibling(target.getFileName() + ".tmp");
        try {
            objectMapper.writeValue(temporary.toFile(), report);
            atomicReplace(temporary, target);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write campaign summary to " + target, e);
        }
    }

    /**
     * Replaces {@code targetToReplace} with {@code source}.
     * <p>
     * Requests an atomic move first. If unsupported by the filesystem, falls back
     * to a regular replacement, which does not guarantee atomic visibility.
     *
     * @throws IOException if either replacement attempt fails
     */
    private static void atomicReplace(Path source, Path targetToReplace) throws IOException {
        try {
            Files.move(source, targetToReplace, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // ATOMIC_MOVE unsupported; fallback replacement may be observed non-atomically.
            Files.move(source, targetToReplace, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
