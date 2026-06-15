package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.EnrichedScenarioRecord;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class ScenarioCatalogReader {
    private final ObjectMapper mapper;

    public ScenarioCatalogReader() {
        this(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    ScenarioCatalogReader(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    public List<CatalogScenarioRecord> read(ScenarioExecutorOptions options) {
        Path catalog = resolveCatalog(options);
        String kind = catalog.getFileName().toString().contains("enriched") ? "ENRICHED" : "STATIC";
        try {
            List<String> lines = Files.exists(catalog) ? Files.readAllLines(catalog) : List.of();
            List<CatalogScenarioRecord> records = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                if ("ENRICHED".equals(kind)) {
                    EnrichedScenarioRecord enriched = mapper.readValue(line, EnrichedScenarioRecord.class);
                    DynamicEvidenceJoinStatus status = enriched.dynamicEvidence() == null ? null : enriched.dynamicEvidence().joinStatus();
                    records.add(new CatalogScenarioRecord(enriched.scenarioPlan(), status, i + 1, kind, catalog.toString()));
                } else {
                    records.add(new CatalogScenarioRecord(mapper.readValue(line, ScenarioPlan.class), null, i + 1, kind, catalog.toString()));
                }
            }
            return records;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read scenario catalog " + catalog, e);
        }
    }

    private Path resolveCatalog(ScenarioExecutorOptions options) {
        if (options.catalogPath() != null) {
            return options.catalogPath();
        }
        Path runDirectory = options.runDirectory() == null ? Path.of(".") : options.runDirectory();
        Path enriched = runDirectory.resolve("scenario-catalog-enriched.jsonl");
        if (Files.exists(enriched)) {
            return enriched;
        }
        return runDirectory.resolve("scenario-catalog.jsonl");
    }
}
