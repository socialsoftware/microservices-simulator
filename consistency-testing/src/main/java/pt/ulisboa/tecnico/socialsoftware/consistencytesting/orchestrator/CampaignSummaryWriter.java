package pt.ulisboa.tecnico.socialsoftware.consistencytesting.orchestrator;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.AccessDeniedException;
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
    /** Increasing delays between retries after a transient access denial. */
    private static final long[] ACCESS_DENIED_RETRY_DELAYS_MILLIS = { 25L, 50L, 100L, 200L };

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
        for (int attempt = 0; attempt <= ACCESS_DENIED_RETRY_DELAYS_MILLIS.length; attempt++) {
            try {
                moveWithAtomicFallback(source, targetToReplace);
                return;
            } catch (AccessDeniedException e) {
                if (attempt == ACCESS_DENIED_RETRY_DELAYS_MILLIS.length) {
                    throw e; // max retries reached; propagate the exception
                }
                pauseBeforeRetry(ACCESS_DENIED_RETRY_DELAYS_MILLIS[attempt], e);
            }
        }
    }

    /**
     * Moves {@code source} into {@code targetToReplace} atomically, falling back to
     * non-atomic moves when unsupported.
     */
    private static void moveWithAtomicFallback(Path source, Path targetToReplace) throws IOException {
        try {
            Files.move(source, targetToReplace, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // ATOMIC_MOVE unsupported; fallback replacement may be observed non-atomically.
            Files.move(source, targetToReplace, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    /**
     * Waits {@code delayMillis} milliseconds and preserves the access denial
     * exception if interrupted.
     */
    private static void pauseBeforeRetry(long delayMillis, AccessDeniedException original) throws IOException {
        try {
            Thread.sleep(delayMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            IOException interrupted = new IOException("Interrupted while retrying campaign summary replacement", e);
            interrupted.addSuppressed(original); // preserve the original exception for debugging
            throw interrupted;
        }
    }
}
