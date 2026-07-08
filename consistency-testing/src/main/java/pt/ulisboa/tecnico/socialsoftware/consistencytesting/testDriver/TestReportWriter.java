package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Serializes {@link TestReport}s as indented JSON under a reports directory,
 * one file per report. This is the durable, human-readable record of what a run
 * found, the artifact a developer (or evaluation tool) inspects.
 */
final class TestReportWriter {

    private final Path reportsDirectory;
    private final ObjectMapper objectMapper;
    private final AtomicInteger reportCounter = new AtomicInteger();

    TestReportWriter(Path reportsDirectory) {
        this.reportsDirectory = reportsDirectory;
        this.objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

        try {
            Files.createDirectories(reportsDirectory);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not create reports directory: " + reportsDirectory, e);
        }
    }

    /**
     * Writes {@code report} to a uniquely-named JSON file and returns its path.
     */
    Path write(TestReport report) {
        Path target = reportsDirectory.resolve("test-report-%05d.json".formatted(reportCounter.incrementAndGet()));
        try {
            objectMapper.writeValue(target.toFile(), report);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not write test report to " + target, e);
        }
        return target;
    }
}
