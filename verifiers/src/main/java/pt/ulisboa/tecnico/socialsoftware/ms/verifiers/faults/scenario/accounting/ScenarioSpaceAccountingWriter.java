package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Objects;

public final class ScenarioSpaceAccountingWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final ObjectWriter PRETTY_WRITER = OBJECT_MAPPER.writerWithDefaultPrettyPrinter();

    public void write(ScenarioSpaceAccountingReport report, Path accountingPath) throws IOException {
        ScenarioSpaceAccountingReport safeReport = Objects.requireNonNull(report, "report");
        Path safeAccountingPath = Objects.requireNonNull(accountingPath, "accountingPath");
        Path parent = safeAccountingPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        try (BufferedWriter writer = Files.newBufferedWriter(
                safeAccountingPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writer.write(PRETTY_WRITER.writeValueAsString(safeReport));
            writer.newLine();
        }
    }
}
