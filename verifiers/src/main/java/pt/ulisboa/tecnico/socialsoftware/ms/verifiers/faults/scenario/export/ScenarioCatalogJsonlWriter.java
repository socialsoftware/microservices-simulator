package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RejectedInputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public final class ScenarioCatalogJsonlWriter {

    public static final String DEFAULT_FAULT_SCENARIO_FILE = "fault-scenario-catalog.jsonl";
    public static final String DEFAULT_REJECTED_INPUT_FILE = "workload-catalog-rejected-inputs.jsonl";
    public static final String DEFAULT_ACCOUNTING_FILE = "scenario-space-accounting.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ScenarioCatalogManifest write(WorkloadGenerationResult result,
                                         Path workloadCatalogPath,
                                         Path manifestPath,
                                         String generatedAt) throws IOException {
        Path parent = workloadCatalogPath == null ? null : workloadCatalogPath.getParent();
        Path base = parent == null ? Path.of(".") : parent;
        return write(
                result,
                workloadCatalogPath,
                base.resolve(DEFAULT_FAULT_SCENARIO_FILE),
                manifestPath,
                base.resolve(DEFAULT_REJECTED_INPUT_FILE),
                base.resolve(DEFAULT_ACCOUNTING_FILE),
                null,
                generatedAt);
    }

    public ScenarioCatalogManifest write(WorkloadGenerationResult result,
                                         Path workloadCatalogPath,
                                         Path manifestPath,
                                         Path rejectedInputsPath,
                                         String generatedAt) throws IOException {
        Path parent = workloadCatalogPath == null ? null : workloadCatalogPath.getParent();
        Path base = parent == null ? Path.of(".") : parent;
        return write(
                result,
                workloadCatalogPath,
                base.resolve(DEFAULT_FAULT_SCENARIO_FILE),
                manifestPath,
                rejectedInputsPath,
                base.resolve(DEFAULT_ACCOUNTING_FILE),
                null,
                generatedAt);
    }

    public ScenarioCatalogManifest write(WorkloadGenerationResult result,
                                         Path workloadCatalogPath,
                                         Path faultScenarioCatalogPath,
                                         Path manifestPath,
                                         Path rejectedInputsPath,
                                         Path accountingPath,
                                         ScenarioSpaceAccountingReport accountingReport,
                                         String generatedAt) throws IOException {
        WorkloadGenerationResult safeResult = Objects.requireNonNull(result, "result");
        Path safeWorkloadPath = Objects.requireNonNull(workloadCatalogPath, "workloadCatalogPath");
        Path safeFaultScenarioPath = Objects.requireNonNull(faultScenarioCatalogPath, "faultScenarioCatalogPath");
        Path safeManifestPath = Objects.requireNonNull(manifestPath, "manifestPath");
        Path safeRejectedInputsPath = Objects.requireNonNull(rejectedInputsPath, "rejectedInputsPath");
        Path safeAccountingPath = Objects.requireNonNull(accountingPath, "accountingPath");
        String safeGeneratedAt = requireGeneratedAt(generatedAt);

        createParentDirectories(safeWorkloadPath);
        createParentDirectories(safeFaultScenarioPath);
        createParentDirectories(safeManifestPath);
        createParentDirectories(safeRejectedInputsPath);
        createParentDirectories(safeAccountingPath);

        int workloadsExported = writeWorkloads(safeResult, safeWorkloadPath);
        int faultScenariosExported = writeEmptyFaultScenarios(safeFaultScenarioPath);
        int rejectedInputsExported = writeRejectedInputs(safeResult.rejectedInputVariants(), safeRejectedInputsPath);
        ScenarioSpaceAccountingReport baseAccounting = accountingReport == null
                ? ScenarioSpaceAccountingReport.placeholder("unknown", safeResult.effectiveConfig(), workloadsExported)
                : accountingReport;
        ScenarioSpaceAccountingReport packageAccounting = baseAccounting.withCatalogPackage(
                safeResult.workloadPlans(), Integer.toString(faultScenariosExported));
        new ScenarioSpaceAccountingWriter().write(packageAccounting, safeAccountingPath);

        ScenarioCatalogManifest manifest = buildManifest(
                safeResult,
                safeWorkloadPath,
                safeFaultScenarioPath,
                safeManifestPath,
                safeRejectedInputsPath,
                safeAccountingPath,
                safeGeneratedAt,
                workloadsExported,
                faultScenariosExported,
                rejectedInputsExported);
        writeManifest(manifest, safeManifestPath);
        return manifest;
    }

    private static int writeWorkloads(WorkloadGenerationResult result, Path workloadPath) throws IOException {
        List<WorkloadPlan> workloads = result.workloadPlans().stream()
                .sorted(Comparator.comparing(WorkloadPlan::kind)
                        .thenComparing(WorkloadPlan::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
        try (BufferedWriter writer = Files.newBufferedWriter(
                workloadPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (WorkloadPlan workload : workloads) {
                writer.write(OBJECT_MAPPER.writeValueAsString(Objects.requireNonNull(workload, "workloadPlan")));
                writer.write('\n');
            }
        }
        return workloads.size();
    }

    private static int writeEmptyFaultScenarios(Path faultScenarioPath) throws IOException {
        Files.writeString(
                faultScenarioPath,
                "",
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE);
        return 0;
    }

    private static int writeRejectedInputs(List<RejectedInputVariant> rejectedInputVariants,
                                           Path rejectedInputsPath) throws IOException {
        List<RejectedInputVariant> safeRejectedInputs = rejectedInputVariants == null ? List.of() : rejectedInputVariants;
        try (BufferedWriter writer = Files.newBufferedWriter(
                rejectedInputsPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (RejectedInputVariant rejected : safeRejectedInputs) {
                writer.write(OBJECT_MAPPER.writeValueAsString(toRejectedInputLine(Objects.requireNonNull(rejected))));
                writer.write('\n');
            }
        }
        return safeRejectedInputs.size();
    }

    private static RejectedInputLine toRejectedInputLine(RejectedInputVariant rejected) {
        return new RejectedInputLine(
                RejectedInputVariant.SCHEMA_VERSION,
                rejected.inputVariant(),
                rejected.rejectionReason(),
                rejected.warnings());
    }

    private static ScenarioCatalogManifest buildManifest(WorkloadGenerationResult result,
                                                          Path workloadPath,
                                                          Path faultScenarioPath,
                                                          Path manifestPath,
                                                          Path rejectedInputsPath,
                                                          Path accountingPath,
                                                          String generatedAt,
                                                          int workloadsExported,
                                                          int faultScenariosExported,
                                                          int rejectedInputsExported) {
        LinkedHashMap<String, String> counts = decimalCounts(result.counts());
        counts.put("workloadsExported", Integer.toString(workloadsExported));
        counts.put("faultScenariosExported", Integer.toString(faultScenariosExported));
        counts.put("rejectedInputsExported", Integer.toString(rejectedInputsExported));
        return new ScenarioCatalogManifest(
                ScenarioCatalogManifest.SCHEMA_VERSION,
                generatedAt,
                result.effectiveConfig(),
                "STATIC_ANALYSIS",
                "INPUT_READINESS_AND_STRUCTURAL_ADMISSIBILITY",
                counts,
                result.warnings(),
                artifact("WORKLOAD_CATALOG", WorkloadPlan.SCHEMA_VERSION, workloadPath, workloadsExported),
                artifact("FAULT_SCENARIO_CATALOG", ScenarioCatalogManifest.FAULT_SCENARIO_SCHEMA_VERSION, faultScenarioPath, faultScenariosExported),
                artifact("SCENARIO_SPACE_ACCOUNTING", ScenarioSpaceAccountingReport.SCHEMA_VERSION, accountingPath, 1),
                artifact("REJECTED_INPUT_DIAGNOSTIC", RejectedInputVariant.SCHEMA_VERSION, rejectedInputsPath, rejectedInputsExported),
                decimalCounts(inputVariantsBySourceMode(result)),
                decimalCounts(inputVariantsAcceptedBySourceMode(result)),
                decimalCounts(inputVariantsRejectedBySourceModeReason(result)));
    }

    private static ScenarioCatalogManifest.ArtifactMetadata artifact(String kind,
                                                                      String schema,
                                                                      Path path,
                                                                      int count) {
        return new ScenarioCatalogManifest.ArtifactMetadata(kind, schema, path.toString(), Integer.toString(count));
    }

    private static LinkedHashMap<String, Integer> inputVariantsBySourceMode(WorkloadGenerationResult result) {
        LinkedHashMap<String, InputVariant> uniqueInputs = acceptedInputsById(result);
        result.rejectedInputVariants().forEach(rejected -> {
            InputVariant input = rejected.inputVariant();
            if (input != null && input.deterministicId() != null) {
                uniqueInputs.putIfAbsent(input.deterministicId(), input);
            }
        });
        return countBySourceMode(uniqueInputs.values().stream().toList());
    }

    private static LinkedHashMap<String, Integer> inputVariantsAcceptedBySourceMode(WorkloadGenerationResult result) {
        return countBySourceMode(acceptedInputsById(result).values().stream().toList());
    }

    private static LinkedHashMap<String, InputVariant> acceptedInputsById(WorkloadGenerationResult result) {
        LinkedHashMap<String, InputVariant> uniqueInputs = new LinkedHashMap<>();
        result.workloadPlans().forEach(workload -> workload.acceptedInputs().forEach(input -> {
            if (input.deterministicId() != null) {
                uniqueInputs.putIfAbsent(input.deterministicId(), input);
            }
        }));
        return uniqueInputs;
    }

    private static LinkedHashMap<String, Integer> countBySourceMode(List<InputVariant> inputs) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>();
        for (SourceMode mode : SourceMode.values()) {
            counts.put(mode.name(), 0);
        }
        inputs.stream()
                .sorted(Comparator.comparing(InputVariant::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .forEach(input -> counts.merge(input.sourceMode().name(), 1, Integer::sum));
        return counts;
    }

    private static LinkedHashMap<String, Integer> inputVariantsRejectedBySourceModeReason(WorkloadGenerationResult result) {
        return result.rejectedInputVariants().stream()
                .filter(rejected -> rejected.rejectionReason() != null)
                .collect(Collectors.toMap(
                        rejected -> rejected.rejectionReason().name(),
                        ignored -> 1,
                        Integer::sum,
                        LinkedHashMap::new));
    }

    private static LinkedHashMap<String, String> decimalCounts(Map<String, Integer> values) {
        LinkedHashMap<String, String> decimal = new LinkedHashMap<>();
        if (values != null) {
            values.forEach((key, value) -> decimal.put(key, Integer.toString(value == null ? 0 : value)));
        }
        return decimal;
    }

    private static void writeManifest(ScenarioCatalogManifest manifest, Path manifestPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                manifestPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            writer.write(OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(manifest));
            writer.write('\n');
        }
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static String requireGeneratedAt(String generatedAt) {
        if (generatedAt == null || generatedAt.isBlank()) {
            throw new IllegalArgumentException("generatedAt must be provided");
        }
        return generatedAt;
    }

    private record RejectedInputLine(
            String schemaVersion,
            InputVariant input,
            Object rejectionReason,
            List<String> rejectionWarnings) {
    }
}
