package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Temporary v2 executor reader. It deliberately does not discover or consume v3 packages. */
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
        rejectV3ArtifactPath(catalog);
        String kind = catalog.getFileName().toString().contains("enriched") ? "ENRICHED" : "STATIC";
        try {
            List<String> lines = Files.exists(catalog) ? Files.readAllLines(catalog) : List.of();
            List<CatalogScenarioRecord> records = new ArrayList<>();
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line == null || line.isBlank()) {
                    continue;
                }
                JsonNode node = mapper.readTree(line);
                JsonNode legacyPlanNode = "ENRICHED".equals(kind) ? node.path("scenarioPlan") : node;
                String schema = legacyPlanNode.path("schemaVersion").asText(null);
                if (!LegacyScenarioPlan.SCHEMA_VERSION.equals(schema)) {
                    throw unsupportedSchema(schema);
                }
                LegacyScenarioPlan plan = mapper.treeToValue(legacyPlanNode, LegacyScenarioPlan.class);
                DynamicEvidenceJoinStatus status = null;
                if ("ENRICHED".equals(kind)) {
                    String statusText = node.path("dynamicEvidence").path("joinStatus").asText(null);
                    status = statusText == null ? null : DynamicEvidenceJoinStatus.valueOf(statusText);
                }
                records.add(new CatalogScenarioRecord(plan, status, i + 1, kind, catalog.toString()));
            }
            return records;
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read legacy scenario catalog " + catalog, e);
        }
    }

    private Path resolveCatalog(ScenarioExecutorOptions options) {
        if (options.catalogPath() != null) {
            return options.catalogPath();
        }
        Path runDirectory = options.runDirectory() == null ? Path.of(".") : options.runDirectory();
        rejectV3Manifest(runDirectory.resolve("scenario-catalog-manifest.json"));
        Path enriched = runDirectory.resolve("scenario-catalog-enriched.jsonl");
        if (Files.exists(enriched)) {
            return enriched;
        }
        return runDirectory.resolve("scenario-catalog.jsonl");
    }

    private void rejectV3Manifest(Path manifestPath) {
        if (!Files.isRegularFile(manifestPath)) {
            return;
        }
        try {
            JsonNode manifest = mapper.readTree(Files.readString(manifestPath));
            if (ScenarioCatalogManifest.SCHEMA_VERSION.equals(manifest.path("schemaVersion").asText(null))) {
                throw unsupportedSchema(ScenarioCatalogManifest.SCHEMA_VERSION);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to inspect scenario catalog manifest " + manifestPath, exception);
        }
    }

    private void rejectV3ArtifactPath(Path path) {
        String name = path.getFileName() == null ? "" : path.getFileName().toString();
        if ("workload-catalog.jsonl".equals(name)
                || "fault-scenario-catalog.jsonl".equals(name)
                || "scenario-catalog-manifest.json".equals(name)) {
            throw unsupportedSchema(WorkloadPlan.SCHEMA_VERSION);
        }
    }

    private IllegalArgumentException unsupportedSchema(String schema) {
        return new IllegalArgumentException("Legacy executor does not support schema '" + schema
                + "' or v3 WorkloadPlan/FaultScenario packages; persisted v3 replay is introduced in slice S6");
    }
}
