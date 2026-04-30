package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RejectedInputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioCatalogManifest;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ScenarioPlan;
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
import java.util.Objects;
import java.util.stream.Collectors;

public final class ScenarioCatalogJsonlWriter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public ScenarioCatalogManifest write(ScenarioGenerationResult result,
                                         Path catalogPath,
                                         Path manifestPath,
                                         String generatedAt) throws IOException {
        Path rejectedPath = catalogPath == null || catalogPath.getParent() == null
                ? Path.of("scenario-catalog-rejected-inputs.jsonl")
                : catalogPath.getParent().resolve("scenario-catalog-rejected-inputs.jsonl");
        return write(result, catalogPath, manifestPath, rejectedPath, generatedAt);
    }

    public ScenarioCatalogManifest write(ScenarioGenerationResult result,
                                         Path catalogPath,
                                         Path manifestPath,
                                         Path rejectedInputsPath,
                                         String generatedAt) throws IOException {
        ScenarioGenerationResult safeResult = Objects.requireNonNull(result, "result");
        Path safeCatalogPath = Objects.requireNonNull(catalogPath, "catalogPath");
        Path safeManifestPath = Objects.requireNonNull(manifestPath, "manifestPath");
        Path safeRejectedInputsPath = Objects.requireNonNull(rejectedInputsPath, "rejectedInputsPath");
        String safeGeneratedAt = requireGeneratedAt(generatedAt);

        createParentDirectories(safeCatalogPath);
        createParentDirectories(safeManifestPath);
        createParentDirectories(safeRejectedInputsPath);

        int scenariosExported = writeCatalog(safeResult, safeCatalogPath);
        int rejectedInputsExported = writeRejectedInputs(safeResult.rejectedInputVariants(), safeRejectedInputsPath);

        ScenarioCatalogManifest manifest = buildManifest(
                safeResult,
                safeCatalogPath,
                safeManifestPath,
                safeRejectedInputsPath,
                safeGeneratedAt,
                scenariosExported,
                rejectedInputsExported);
        writeManifest(manifest, safeManifestPath);
        return manifest;
    }

    private static int writeCatalog(ScenarioGenerationResult result, Path catalogPath) throws IOException {
        int scenariosExported = 0;
        try (BufferedWriter writer = Files.newBufferedWriter(
                catalogPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (ScenarioPlan plan : result.scenarioPlans()) {
                Objects.requireNonNull(plan, "scenarioPlan");
                writer.write(OBJECT_MAPPER.writeValueAsString(plan));
                writer.write('\n');
                scenariosExported++;
            }
        }
        return scenariosExported;
    }

    private static int writeRejectedInputs(List<RejectedInputVariant> rejectedInputVariants,
                                           Path rejectedInputsPath) throws IOException {
        int rejectedInputsExported = 0;
        List<RejectedInputVariant> safeRejectedInputs = rejectedInputVariants == null ? List.of() : rejectedInputVariants;
        try (BufferedWriter writer = Files.newBufferedWriter(
                rejectedInputsPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            for (RejectedInputVariant rejected : safeRejectedInputs) {
                Objects.requireNonNull(rejected, "rejectedInputVariant");
                writer.write(OBJECT_MAPPER.writeValueAsString(toRejectedInputLine(rejected)));
                writer.write('\n');
                rejectedInputsExported++;
            }
        }
        return rejectedInputsExported;
    }

    private static RejectedInputLine toRejectedInputLine(RejectedInputVariant rejected) {
        InputVariant input = rejected.inputVariant();
        return new RejectedInputLine(
                input == null ? null : input.deterministicId(),
                input == null ? null : input.sagaFqn(),
                input == null ? null : input.sourceClassFqn(),
                input == null ? null : input.sourceMethodName(),
                input == null ? null : input.sourceBindingName(),
                input == null ? null : input.stableSourceText(),
                input == null ? null : input.resolutionStatus(),
                input == null ? null : input.sourceMode(),
                input == null ? null : input.sourceModeConfidence(),
                input == null ? List.of() : input.sourceModeEvidence(),
                rejected.rejectionReason(),
                input == null ? List.of() : input.warnings(),
                rejected.warnings(),
                input == null ? null : input.provenanceText());
    }

    private static ScenarioCatalogManifest buildManifest(ScenarioGenerationResult result,
                                                         Path catalogPath,
                                                         Path manifestPath,
                                                         Path rejectedInputsPath,
                                                         String generatedAt,
                                                         int scenariosExported,
                                                         int rejectedInputsExported) {
        LinkedHashMap<String, Integer> counts = new LinkedHashMap<>(result.counts());
        counts.put("scenariosExported", scenariosExported);
        counts.put("rejectedInputsExported", rejectedInputsExported);
        return new ScenarioCatalogManifest(
                ScenarioPlan.SCHEMA_VERSION,
                generatedAt,
                result.effectiveConfig(),
                counts,
                result.warnings(),
                catalogPath.toString(),
                manifestPath.toString(),
                rejectedInputsPath.toString(),
                inputVariantsBySourceMode(result),
                inputVariantsAcceptedBySourceMode(result),
                inputVariantsRejectedBySourceModeReason(result));
    }

    private static LinkedHashMap<String, Integer> inputVariantsBySourceMode(ScenarioGenerationResult result) {
        LinkedHashMap<String, InputVariant> uniqueInputs = acceptedInputsById(result);
        result.rejectedInputVariants().forEach(rejected -> {
            InputVariant input = rejected.inputVariant();
            if (input != null && input.deterministicId() != null) {
                uniqueInputs.putIfAbsent(input.deterministicId(), input);
            }
        });
        return countBySourceMode(uniqueInputs.values().stream().toList());
    }

    private static LinkedHashMap<String, Integer> inputVariantsAcceptedBySourceMode(ScenarioGenerationResult result) {
        return countBySourceMode(acceptedInputsById(result).values().stream().toList());
    }

    private static LinkedHashMap<String, InputVariant> acceptedInputsById(ScenarioGenerationResult result) {
        LinkedHashMap<String, InputVariant> uniqueInputs = new LinkedHashMap<>();
        result.scenarioPlans().forEach(plan -> plan.inputs().forEach(input -> {
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

    private static LinkedHashMap<String, Integer> inputVariantsRejectedBySourceModeReason(ScenarioGenerationResult result) {
        return result.rejectedInputVariants().stream()
                .filter(rejected -> rejected.rejectionReason() != null)
                .collect(Collectors.toMap(
                        rejected -> rejected.rejectionReason().name(),
                        ignored -> 1,
                        Integer::sum,
                        LinkedHashMap::new));
    }

    private static void writeManifest(ScenarioCatalogManifest manifest, Path manifestPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(
                manifestPath,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE)) {
            OBJECT_MAPPER.writeValue(writer, manifest);
        }
    }

    private static void createParentDirectories(Path path) throws IOException {
        if (path == null) {
            return;
        }
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
            String deterministicId,
            String sagaFqn,
            String sourceClassFqn,
            String sourceMethodName,
            String sourceBindingName,
            String stableSourceText,
            Object resolutionStatus,
            Object sourceMode,
            Object sourceModeConfidence,
            List<String> sourceModeEvidence,
            Object rejectionReason,
            List<String> warnings,
            List<String> rejectionWarnings,
            String provenanceText) {
    }
}
