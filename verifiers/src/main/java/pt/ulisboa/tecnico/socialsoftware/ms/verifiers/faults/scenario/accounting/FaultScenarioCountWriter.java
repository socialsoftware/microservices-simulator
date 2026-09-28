package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.adapter.ScenarioModelAdapterResult;
import java.io.*;
import java.nio.file.*;

/** Compact additive evidence; no executable catalogue files are created. */
public final class FaultScenarioCountWriter {
    public static FaultScenarioCountService.Report write(ScenarioModelAdapterResult model, ScenarioGeneratorConfig config,
                                                        Path directory, long maxStates, long maxTuples) throws IOException {
        Files.createDirectories(directory);
        ObjectMapper json = new ObjectMapper().findAndRegisterModules();
        Path summary = directory.resolve("fault-count-summary.json");
        Path rows = directory.resolve("fault-count-sets.jsonl");
        if (Files.exists(summary) || Files.exists(rows)) throw new IOException("Count output already exists: " + directory);
        json.writeValue(summary.toFile(), java.util.Map.of("status", "RUNNING", "config", config));
        try (var writer = Files.newBufferedWriter(rows, StandardOpenOption.CREATE_NEW)) {
            var service = new FaultScenarioCountService(maxStates, maxTuples);
            var result = service.count(model, config, row -> {
                try { writer.write(json.writeValueAsString(row)); writer.newLine(); writer.flush(); }
                catch (IOException e) { throw new UncheckedIOException(e); }
            });
            Path temporary = directory.resolve("fault-count-summary.json.tmp");
            json.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), result);
            Files.move(temporary, summary, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return result;
        } catch (RuntimeException | IOException e) {
            json.writeValue(summary.toFile(), java.util.Map.of("status", "INCOMPLETE", "diagnostic", e.toString()));
            throw e;
        }
    }
}
