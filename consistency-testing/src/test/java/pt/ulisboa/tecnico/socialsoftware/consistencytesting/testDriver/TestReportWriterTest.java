package pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.fasterxml.jackson.databind.ObjectMapper;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestReport.InterInvariantViolationView;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.testDriver.TestReport.ReadsFromView;

class TestReportWriterTest {

    @Test
    void writesReportAsJsonAndCreatesMissingDirectory(@TempDir Path tempDir) throws Exception {
        Path reportsDir = tempDir.resolve("does-not-exist-yet");
        TestReportWriter writer = new TestReportWriter(reportsDir);

        TestReport report = new TestReport(
                List.of("update-1::getOriginalTournamentStep", "update-1::commitStep"),
                List.of("INTER_INVARIANT_VIOLATION"),
                Map.of("NUMBER_OF_QUESTIONS",
                        List.of(new InterInvariantViolationView("tournament 10 expects 3 questions but quiz has 2"))),
                List.of(new ReadsFromView(
                        "update-1::getTopicsStep", "initialStateSetup::step", "Topic")),
                Map.of("update-1::getOriginalTournamentStep", "SimulatorException: boom!"),
                1);

        Path written = writer.write(report);

        assertTrue(Files.exists(written), "report file should be created");
        assertTrue(Files.isDirectory(reportsDir), "reports directory should be created on demand");

        // round-trips back to an equal report (records serialize/deserialize cleanly)
        TestReport roundTrip = new ObjectMapper().readValue(written.toFile(), TestReport.class);
        assertEquals(report, roundTrip);
    }
}
