package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.WorkloadPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public final class ScenarioCatalogPackageReader {

    private final ObjectMapper objectMapper;

    public ScenarioCatalogPackageReader() {
        this(new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
                .enable(DeserializationFeature.USE_BIG_INTEGER_FOR_INTS));
    }

    ScenarioCatalogPackageReader(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper);
    }

    public PackageContents read(Path manifestPath) {
        Path safeManifestPath = Objects.requireNonNull(manifestPath, "manifestPath").toAbsolutePath().normalize();
        JsonNode manifestNode = readJson(safeManifestPath, "scenario catalog manifest");
        String schema = text(manifestNode, "schemaVersion");
        if (!ScenarioCatalogManifest.SCHEMA_VERSION.equals(schema)) {
            throw unsupportedSchema(schema);
        }

        ScenarioCatalogManifest manifest = treeToValue(manifestNode, ScenarioCatalogManifest.class, "scenario catalog manifest");
        validateArtifactMetadata(manifest.workloadCatalog(), "WORKLOAD_CATALOG", WorkloadPlan.SCHEMA_VERSION);
        validateArtifactMetadata(manifest.faultScenarioCatalog(), "FAULT_SCENARIO_CATALOG", ScenarioCatalogManifest.FAULT_SCENARIO_SCHEMA_VERSION);
        validateArtifactMetadata(manifest.scenarioSpaceAccounting(), "SCENARIO_SPACE_ACCOUNTING", ScenarioSpaceAccountingReport.SCHEMA_VERSION);
        validateArtifactMetadata(manifest.rejectedInputsDiagnostic(), "REJECTED_INPUT_DIAGNOSTIC", null);

        Path workloadPath = resolveArtifact(safeManifestPath, manifest.workloadCatalog().path());
        Path faultScenarioPath = resolveArtifact(safeManifestPath, manifest.faultScenarioCatalog().path());
        Path accountingPath = resolveArtifact(safeManifestPath, manifest.scenarioSpaceAccounting().path());
        Path rejectedInputsPath = resolveArtifact(safeManifestPath, manifest.rejectedInputsDiagnostic().path());
        List<WorkloadPlan> workloads = readWorkloads(workloadPath);
        List<JsonNode> faultScenarios = readFaultScenarios(faultScenarioPath, workloads);
        List<JsonNode> rejectedInputDiagnostics = readRejectedInputDiagnostics(
                rejectedInputsPath, manifest.rejectedInputsDiagnostic().schemaVersion());
        JsonNode accounting = readJson(accountingPath, "scenario-space accounting");
        if (!ScenarioSpaceAccountingReport.SCHEMA_VERSION.equals(text(accounting, "schemaVersion"))) {
            throw new IllegalArgumentException("Linked accounting artifact has unsupported schema '"
                    + text(accounting, "schemaVersion") + "'; expected " + ScenarioSpaceAccountingReport.SCHEMA_VERSION);
        }

        validateRecordCount(manifest.workloadCatalog(), workloads.size());
        validateRecordCount(manifest.faultScenarioCatalog(), faultScenarios.size());
        validateRecordCount(manifest.scenarioSpaceAccounting(), 1);
        validateRecordCount(manifest.rejectedInputsDiagnostic(), rejectedInputDiagnostics.size());
        return new PackageContents(manifest, workloads, faultScenarios, rejectedInputDiagnostics, accounting,
                workloadPath, faultScenarioPath, rejectedInputsPath, accountingPath);
    }

    private List<WorkloadPlan> readWorkloads(Path path) {
        List<WorkloadPlan> workloads = new ArrayList<>();
        List<String> lines = readLines(path, "workload catalog");
        WorkloadPlanValidator validator = new WorkloadPlanValidator();
        Set<String> ids = new HashSet<>();
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = readJsonLine(line, path, index + 1);
            String schema = text(node, "schemaVersion");
            if (!WorkloadPlan.SCHEMA_VERSION.equals(schema)) {
                throw unsupportedSchema(schema);
            }
            WorkloadPlan workload = treeToValue(node, WorkloadPlan.class,
                    "workload record " + path + ":" + (index + 1));
            WorkloadPlanValidator.ValidationResult validation = validator.validate(workload);
            if (!validation.valid()) {
                throw new IllegalArgumentException("Invalid WorkloadPlan " + workload.deterministicId()
                        + ": " + validation.diagnostics());
            }
            if (!ids.add(workload.deterministicId())) {
                throw new IllegalArgumentException("Duplicate WorkloadPlan id " + workload.deterministicId());
            }
            workloads.add(workload);
        }
        return List.copyOf(workloads);
    }

    private List<JsonNode> readFaultScenarios(Path path, List<WorkloadPlan> workloads) {
        Set<String> workloadIds = new HashSet<>();
        workloads.forEach(workload -> workloadIds.add(workload.deterministicId()));
        List<JsonNode> records = new ArrayList<>();
        Set<String> ids = new HashSet<>();
        List<String> lines = readLines(path, "fault-scenario catalog");
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = readJsonLine(line, path, index + 1);
            String schema = text(node, "schemaVersion");
            if (!ScenarioCatalogManifest.FAULT_SCENARIO_SCHEMA_VERSION.equals(schema)) {
                throw unsupportedSchema(schema);
            }
            String id = text(node, "deterministicId");
            String workloadPlanId = text(node, "workloadPlanId");
            if (id == null || !ids.add(id)) {
                throw new IllegalArgumentException("FaultScenario has missing or duplicate deterministicId at " + path + ":" + (index + 1));
            }
            if (!workloadIds.contains(workloadPlanId)) {
                throw new IllegalArgumentException("FaultScenario " + id + " references missing WorkloadPlan " + workloadPlanId);
            }
            records.add(node);
        }
        return List.copyOf(records);
    }

    private List<JsonNode> readRejectedInputDiagnostics(Path path, String expectedSchema) {
        List<JsonNode> records = new ArrayList<>();
        List<String> lines = readLines(path, "rejected-input diagnostic");
        for (int index = 0; index < lines.size(); index++) {
            String line = lines.get(index);
            if (line == null || line.isBlank()) {
                continue;
            }
            JsonNode node = readJsonLine(line, path, index + 1);
            if (!Objects.equals(expectedSchema, text(node, "schemaVersion"))) {
                throw unsupportedSchema(text(node, "schemaVersion"));
            }
            records.add(node);
        }
        return List.copyOf(records);
    }

    private void validateArtifactMetadata(ScenarioCatalogManifest.ArtifactMetadata artifact,
                                          String expectedKind,
                                          String expectedSchema) {
        if (artifact == null) {
            throw new IllegalArgumentException("Manifest is missing linked artifact metadata for " + expectedKind);
        }
        if (!expectedKind.equals(artifact.artifactKind())) {
            throw new IllegalArgumentException("Manifest artifact kind mismatch: expected " + expectedKind
                    + " but found " + artifact.artifactKind());
        }
        if (expectedSchema != null && !expectedSchema.equals(artifact.schemaVersion())) {
            throw unsupportedSchema(artifact.schemaVersion());
        }
        if (artifact.path() == null) {
            throw new IllegalArgumentException("Manifest artifact " + expectedKind + " has no path");
        }
        exactCount(artifact.recordCount(), expectedKind);
    }

    private void validateRecordCount(ScenarioCatalogManifest.ArtifactMetadata artifact, int actual) {
        BigInteger expected = exactCount(artifact.recordCount(), artifact.artifactKind());
        if (!expected.equals(BigInteger.valueOf(actual))) {
            throw new IllegalArgumentException("Manifest count mismatch for " + artifact.artifactKind()
                    + ": expected " + expected + " but read " + actual);
        }
    }

    private BigInteger exactCount(String value, String label) {
        try {
            BigInteger count = new BigInteger(value);
            if (count.signum() < 0) {
                throw new NumberFormatException("negative");
            }
            return count;
        } catch (RuntimeException exception) {
            throw new IllegalArgumentException("Manifest artifact " + label
                    + " must use a non-negative exact decimal recordCount", exception);
        }
    }

    private Path resolveArtifact(Path manifestPath, String configuredPath) {
        Path path = Path.of(configuredPath);
        if (!path.isAbsolute()) {
            Path parent = manifestPath.getParent();
            path = (parent == null ? path : parent.resolve(path)).normalize();
        }
        if (!Files.isRegularFile(path)) {
            throw new IllegalArgumentException("Linked scenario package artifact does not exist: " + path);
        }
        return path;
    }

    private JsonNode readJson(Path path, String label) {
        try {
            return objectMapper.readTree(Files.readString(path));
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read " + label + " " + path, exception);
        }
    }

    private JsonNode readJsonLine(String line, Path path, int lineNumber) {
        try {
            return objectMapper.readTree(line);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed JSON at " + path + ":" + lineNumber, exception);
        }
    }

    private List<String> readLines(Path path, String label) {
        try {
            return Files.readAllLines(path);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Failed to read " + label + " " + path, exception);
        }
    }

    private <T> T treeToValue(JsonNode node, Class<T> type, String label) {
        try {
            return objectMapper.treeToValue(node, type);
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed " + label, exception);
        }
    }

    private IllegalArgumentException unsupportedSchema(String schema) {
        return new IllegalArgumentException("Unsupported scenario catalog schema '" + schema
                + "'; v3 WorkloadPlan/FaultScenario packages are required and v2 catalogs are not supported");
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() || !value.isTextual() ? null : value.asText();
    }

    public record PackageContents(
            ScenarioCatalogManifest manifest,
            List<WorkloadPlan> workloadPlans,
            List<JsonNode> faultScenarios,
            List<JsonNode> rejectedInputDiagnostics,
            JsonNode accounting,
            Path workloadCatalogPath,
            Path faultScenarioCatalogPath,
            Path rejectedInputsPath,
            Path accountingPath) {
        public PackageContents {
            workloadPlans = workloadPlans == null ? List.of() : List.copyOf(workloadPlans);
            faultScenarios = faultScenarios == null ? List.of() : List.copyOf(faultScenarios);
            rejectedInputDiagnostics = rejectedInputDiagnostics == null ? List.of() : List.copyOf(rejectedInputDiagnostics);
        }
    }
}
