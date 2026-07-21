package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.FaultScenarioValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.WorkloadPlanValidator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingReport;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.accounting.ScenarioSpaceAccountingWriter;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ComputedVectorRecovery;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EagerFaultScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioVectorSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RejectedInputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadMaterializability;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state.SourceMode;

import java.io.BufferedWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ScenarioCatalogJsonlWriter {

    public static final String DEFAULT_FAULT_SCENARIO_FILE = "fault-scenario-catalog.jsonl";
    public static final String DEFAULT_REJECTED_INPUT_FILE = "workload-catalog-rejected-inputs.jsonl";
    public static final String DEFAULT_ACCOUNTING_FILE = "scenario-space-accounting.json";

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ScenarioCatalogManifest write(EagerFaultScenarioGenerationResult result,
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

    public ScenarioCatalogManifest write(EagerFaultScenarioGenerationResult result,
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

    public ScenarioCatalogManifest write(EagerFaultScenarioGenerationResult result,
                                         Path workloadCatalogPath,
                                         Path faultScenarioCatalogPath,
                                         Path manifestPath,
                                         Path rejectedInputsPath,
                                         Path accountingPath,
                                         ScenarioSpaceAccountingReport accountingReport,
                                         String generatedAt) throws IOException {
        EagerFaultScenarioGenerationResult safeResult = Objects.requireNonNull(result, "result");
        Path safeWorkloadPath = Objects.requireNonNull(workloadCatalogPath, "workloadCatalogPath");
        Path safeFaultScenarioPath = Objects.requireNonNull(faultScenarioCatalogPath, "faultScenarioCatalogPath");
        Path safeManifestPath = Objects.requireNonNull(manifestPath, "manifestPath");
        Path safeRejectedInputsPath = Objects.requireNonNull(rejectedInputsPath, "rejectedInputsPath");
        Path safeAccountingPath = Objects.requireNonNull(accountingPath, "accountingPath");
        String safeGeneratedAt = requireGeneratedAt(generatedAt);
        validateGenerationResult(safeResult);

        createParentDirectories(safeWorkloadPath);
        createParentDirectories(safeFaultScenarioPath);
        createParentDirectories(safeManifestPath);
        createParentDirectories(safeRejectedInputsPath);
        createParentDirectories(safeAccountingPath);

        int workloadsExported = writeWorkloads(safeResult, safeWorkloadPath);
        int faultScenariosExported = writeFaultScenarios(safeResult, safeFaultScenarioPath);
        int rejectedInputsExported = writeRejectedInputs(safeResult.rejectedInputVariants(), safeRejectedInputsPath);
        ScenarioSpaceAccountingReport baseAccounting = accountingReport == null
                ? ScenarioSpaceAccountingReport.placeholder("unknown", safeResult.effectiveConfig(), workloadsExported)
                : accountingReport;
        ScenarioSpaceAccountingReport packageAccounting = baseAccounting.withCatalogPackage(safeResult);
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

    private static int writeWorkloads(EagerFaultScenarioGenerationResult result, Path workloadPath) throws IOException {
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

    private static int writeFaultScenarios(EagerFaultScenarioGenerationResult result,
                                           Path faultScenarioPath) throws IOException {
        List<FaultScenario> scenarios = result.faultScenarios().stream()
                .sorted(Comparator.comparing(FaultScenario::workloadPlanId, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(FaultScenario::assignedVector, Comparator.nullsFirst(String::compareTo))
                        .thenComparing(FaultScenario::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
        try (BufferedWriter writer = Files.newBufferedWriter(
                faultScenarioPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (FaultScenario scenario : scenarios) {
                writer.write(OBJECT_MAPPER.writeValueAsString(Objects.requireNonNull(scenario, "faultScenario")));
                writer.write('\n');
            }
        }
        return scenarios.size();
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

    private static void validateGenerationResult(EagerFaultScenarioGenerationResult result) {
        Map<String, WorkloadPlan> workloadsById = new LinkedHashMap<>();
        WorkloadPlanValidator workloadValidator = new WorkloadPlanValidator();
        for (WorkloadPlan workload : result.workloadPlans()) {
            WorkloadPlanValidator.ValidationResult validation = workloadValidator.validate(workload);
            if (!validation.valid()) {
                throw new IllegalArgumentException("Invalid WorkloadPlan before package publication: " + validation.diagnostics());
            }
            if (workloadsById.putIfAbsent(workload.deterministicId(), workload) != null) {
                throw new IllegalArgumentException("Duplicate WorkloadPlan id " + workload.deterministicId());
            }
        }

        Map<String, WorkloadMaterializability> materializabilityById = new LinkedHashMap<>();
        for (WorkloadMaterializability materializability : result.workloadMaterializability()) {
            if (!workloadsById.containsKey(materializability.workloadPlanId())
                    || materializabilityById.putIfAbsent(materializability.workloadPlanId(), materializability) != null) {
                throw new IllegalArgumentException("Invalid workload materializability diagnostic for "
                        + materializability.workloadPlanId());
            }
        }
        if (!materializabilityById.keySet().equals(workloadsById.keySet())) {
            throw new IllegalArgumentException("Every WorkloadPlan requires exactly one materializability diagnostic");
        }

        Map<String, FaultScenarioVectorSource> computedVectorSources = new LinkedHashMap<>();
        Map<String, Integer> writtenByVector = new LinkedHashMap<>();
        for (ComputedVectorRecovery vector : result.computedVectors()) {
            WorkloadMaterializability materializability = materializabilityById.get(vector.workloadPlanId());
            String key = vector.workloadPlanId() + "\u0000" + vector.assignedVector();
            if (materializability == null || !materializability.materializable()
                    || computedVectorSources.putIfAbsent(key, vector.vectorSource()) != null) {
                throw new IllegalArgumentException("Invalid or duplicate computed vector " + key);
            }
            if (vector.vectorSource() == null
                    || vector.writtenScheduleCount() <= 0
                    || vector.writtenScheduleCount() > result.recoveryScheduleCap()
                    || vector.uncappedScheduleCount().compareTo(BigInteger.valueOf(vector.writtenScheduleCount())) < 0) {
                throw new IllegalArgumentException("Invalid computed recovery counts for " + key);
            }
            if (vector.vectorSource() == FaultScenarioVectorSource.EAGER_ALL_ZERO
                    && (!BigInteger.ONE.equals(vector.uncappedScheduleCount()) || vector.writtenScheduleCount() != 1)) {
                throw new IllegalArgumentException("Eager all-zero vector must have exact uncapped/written counts 1/1 for " + key);
            }
            writtenByVector.put(key, vector.writtenScheduleCount());
        }

        Map<String, Integer> actualByVector = new LinkedHashMap<>();
        Set<String> faultScenarioIds = new HashSet<>();
        FaultScenarioValidator faultScenarioValidator = new FaultScenarioValidator();
        for (FaultScenario scenario : result.faultScenarios()) {
            WorkloadPlan workload = workloadsById.get(scenario.workloadPlanId());
            FaultScenarioValidator.ValidationResult validation = faultScenarioValidator.validate(scenario, workload);
            if (!validation.valid()) {
                throw new IllegalArgumentException("Invalid FaultScenario before package publication: " + validation.diagnostics());
            }
            if (!faultScenarioIds.add(scenario.deterministicId())) {
                throw new IllegalArgumentException("Duplicate FaultScenario id " + scenario.deterministicId());
            }
            actualByVector.merge(scenario.workloadPlanId() + "\u0000" + scenario.assignedVector(), 1, Integer::sum);
        }
        if (!actualByVector.equals(writtenByVector)) {
            throw new IllegalArgumentException("Computed vector accounting does not match FaultScenario records");
        }

        Map<String, FaultScenarioVectorSource> expectedVectorSources = new LinkedHashMap<>();
        for (WorkloadPlan workload : result.workloadPlans()) {
            WorkloadMaterializability materializability = materializabilityById.get(workload.deterministicId());
            if (!materializability.materializable()) {
                continue;
            }
            int slotCount = workload.faultSlots().size();
            String allZero = "0".repeat(slotCount);
            expectedVectorSources.put(
                    workload.deterministicId() + "\u0000" + allZero,
                    FaultScenarioVectorSource.EAGER_ALL_ZERO);
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                char[] singlePoint = allZero.toCharArray();
                singlePoint[slotIndex] = '1';
                expectedVectorSources.put(
                        workload.deterministicId() + "\u0000" + new String(singlePoint),
                        FaultScenarioVectorSource.EAGER_SINGLE_POINT);
            }
        }
        if (!computedVectorSources.equals(expectedVectorSources)) {
            throw new IllegalArgumentException("Eager vector coverage mismatch: expected "
                    + expectedVectorSources + " but found " + computedVectorSources);
        }
    }

    private static ScenarioCatalogManifest buildManifest(EagerFaultScenarioGenerationResult result,
                                                          Path workloadPath,
                                                          Path faultScenarioPath,
                                                          Path manifestPath,
                                                          Path rejectedInputsPath,
                                                          Path accountingPath,
                                                          String generatedAt,
                                                          int workloadsExported,
                                                          int faultScenariosExported,
                                                          int rejectedInputsExported) throws IOException {
        LinkedHashMap<String, String> counts = decimalCounts(result.counts());
        long materializableWorkloads = result.workloadMaterializability().stream()
                .filter(WorkloadMaterializability::materializable)
                .count();
        BigInteger uncappedSum = result.computedVectors().stream()
                .map(ComputedVectorRecovery::uncappedScheduleCount)
                .reduce(BigInteger.ZERO, BigInteger::add);
        BigInteger writtenSum = result.computedVectors().stream()
                .map(vector -> BigInteger.valueOf(vector.writtenScheduleCount()))
                .reduce(BigInteger.ZERO, BigInteger::add);
        counts.put("workloadsExported", Integer.toString(workloadsExported));
        counts.put("materializableWorkloadPlans", Long.toString(materializableWorkloads));
        counts.put("nonMaterializableWorkloadPlans", Long.toString(workloadsExported - materializableWorkloads));
        counts.put("computedEagerVectors", Integer.toString(result.computedVectors().size()));
        counts.put("computedOnDemandVectors", "0");
        counts.put("computedVectors", Integer.toString(result.computedVectors().size()));
        counts.put("computedVectorUncappedScheduleSum", uncappedSum.toString());
        counts.put("computedVectorWrittenScheduleSum", writtenSum.toString());
        counts.put("faultScenariosExported", Integer.toString(faultScenariosExported));
        counts.put("rejectedInputsExported", Integer.toString(rejectedInputsExported));
        return new ScenarioCatalogManifest(
                ScenarioCatalogManifest.SCHEMA_VERSION,
                generatedAt,
                result.effectiveConfig(),
                "STATIC_ANALYSIS",
                "INPUT_READINESS_AND_STRUCTURAL_ADMISSIBILITY_RUNTIME_MATERIALIZATION_UNPROVEN",
                result.recoveryScheduleCap(),
                "EAGER_ALL_ZERO_AND_SINGLE_POINT",
                result.workloadMaterializability(),
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
                                                                      int count) throws IOException {
        return new ScenarioCatalogManifest.ArtifactMetadata(
                kind,
                schema,
                path.toString(),
                Integer.toString(count),
                sha256(Files.readAllBytes(path)));
    }

    static String sha256(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable", exception);
        }
    }

    private static LinkedHashMap<String, Integer> inputVariantsBySourceMode(EagerFaultScenarioGenerationResult result) {
        LinkedHashMap<String, InputVariant> uniqueInputs = acceptedInputsById(result);
        result.rejectedInputVariants().forEach(rejected -> {
            InputVariant input = rejected.inputVariant();
            if (input != null && input.deterministicId() != null) {
                uniqueInputs.putIfAbsent(input.deterministicId(), input);
            }
        });
        return countBySourceMode(uniqueInputs.values().stream().toList());
    }

    private static LinkedHashMap<String, Integer> inputVariantsAcceptedBySourceMode(EagerFaultScenarioGenerationResult result) {
        return countBySourceMode(acceptedInputsById(result).values().stream().toList());
    }

    private static LinkedHashMap<String, InputVariant> acceptedInputsById(EagerFaultScenarioGenerationResult result) {
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

    private static LinkedHashMap<String, Integer> inputVariantsRejectedBySourceModeReason(EagerFaultScenarioGenerationResult result) {
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
