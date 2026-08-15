package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.Nullable;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Serializes {@link TestReport}s as indented JSON under a reports directory,
 * one file per report. This is the durable, human-readable record of what a run
 * found, the artifact a developer (or evaluation tool) inspects.
 * <p>
 * Reports can be written into a subdirectory of the base directory, which is
 * how a campaign keeps each functionality group's runs together instead of
 * piling a large number of flat files into one directory. Numbering restarts
 * per subdirectory, so a group's files always read {@code 00001..N}.
 */
final class TestReportWriter {

    private final Path reportsDirectory;
    private final ObjectMapper objectMapper;

    /** One counter per (sub)directory, so file numbering is local to it. */
    private final Map<Path, AtomicInteger> reportCounters = new ConcurrentHashMap<>();

    TestReportWriter(Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        createDirectory(reportsDirectory);
    }

    /** Writes {@code report} at the root of the reports directory. */
    Path write(TestReport report) {
        return write(report, null);
    }

    /**
     * Writes {@code report} to a uniquely-named JSON file under
     * {@code subdirectory} (relative to the reports directory; the root when
     * {@code null}) and returns its path.
     */
    Path write(TestReport report, @Nullable Path subdirectory) {
        Path directory = subdirectory == null ? reportsDirectory : reportsDirectory.resolve(subdirectory);
        if (subdirectory != null) {
            createDirectory(directory);
        }

        int reportNumber = reportCounters
                .computeIfAbsent(directory, key -> new AtomicInteger())
                .incrementAndGet();

        Path target = directory.resolve("test-report-%05d.json".formatted(reportNumber));
        try {
            objectMapper.writeValue(target.toFile(), report);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write test report to " + target, e);
        }
        return target;
    }

    private static void createDirectory(Path directory) {
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create reports directory: " + directory, e);
        }
    }
}
