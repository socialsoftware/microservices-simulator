package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.export;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.DynamicEvidenceJoinStatus;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.WorkloadDynamicEvidenceRecord;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model.UnmatchedReason;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class EnrichedScenarioCatalogWriter {
    public static final String MANIFEST_SCHEMA = "microservices-simulator.workload-dynamic-evidence-manifest.v3";
    public static final String JOIN_REPORT_SCHEMA = "microservices-simulator.dynamic-evidence-join-report.v3";

    private final ObjectMapper objectMapper;

    public EnrichedScenarioCatalogWriter() {
        this(new ObjectMapper());
    }

    public EnrichedScenarioCatalogWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper.copy().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }

    public void write(DynamicEvidenceJoinResult joinResult,
                       Path sidecarPath,
                       Path manifestPath,
                       Path joinReportPath,
                       String sourceWorkloadCatalogPath,
                       String dynamicEvidenceRoot,
                       Map<String, ?> effectiveConfig,
                       List<?> testRuns,
                       String generatedAt) throws IOException {
        write(joinResult, sidecarPath, manifestPath, joinReportPath, sourceWorkloadCatalogPath, dynamicEvidenceRoot,
                effectiveConfig, testRuns, generatedAt, Map.of());
    }

    public void write(DynamicEvidenceJoinResult joinResult,
                       Path sidecarPath,
                       Path manifestPath,
                       Path joinReportPath,
                       String sourceWorkloadCatalogPath,
                       String dynamicEvidenceRoot,
                       Map<String, ?> effectiveConfig,
                       List<?> testRuns,
                       String generatedAt,
                       Map<String, Object> reportMetadata) throws IOException {
        createParent(sidecarPath);
        createParent(manifestPath);
        createParent(joinReportPath);

        try (BufferedWriter writer = Files.newBufferedWriter(sidecarPath)) {
            for (WorkloadDynamicEvidenceRecord record : joinResult.records()) {
                writer.write(objectMapper.writeValueAsString(record));
                writer.newLine();
            }
        }

        Map<String, Object> manifest = new LinkedHashMap<>();
        manifest.put("schema", MANIFEST_SCHEMA);
        manifest.put("generatedAt", generatedAt);
        manifest.put("sourceWorkloadCatalogPath", sourceWorkloadCatalogPath);
        manifest.put("sidecarPath", sidecarPath.toString());
        manifest.put("dynamicEvidenceRoot", dynamicEvidenceRoot);
        manifest.put("effectiveDynamicEnrichmentConfig", effectiveConfig == null ? Map.of() : effectiveConfig);
        manifest.put("counts", manifestCounts(joinResult, testRuns));
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(manifestPath.toFile(), manifest);

        Map<String, Object> report = new LinkedHashMap<>();
        report.put("schema", JOIN_REPORT_SCHEMA);
        report.put("generatedAt", generatedAt);
        if (reportMetadata != null) {
            report.putAll(reportMetadata);
        }
        report.put("runStatus", runStatus(testRuns));
        report.put("workloadCatalogPath", sourceWorkloadCatalogPath);
        report.put("dynamicEvidenceRoot", dynamicEvidenceRoot);
        report.put("sidecarPath", sidecarPath.toString());
        report.put("testRuns", testRuns == null ? List.of() : testRuns);
        report.put("counts", reportCounts(joinResult, testRuns));
        report.put("warnings", joinResult.warnings());
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(joinReportPath.toFile(), report);
    }

    private Map<String, Object> manifestCounts(DynamicEvidenceJoinResult joinResult, List<?> testRuns) {
        Map<String, Object> counts = baseStatusCounts(joinResult);
        counts.put("recordCount", joinResult.records().size());
        counts.put("warningCount", joinResult.warnings().size() + joinResult.records().stream().mapToInt(record -> record.dynamicEvidence().warnings().size()).sum());
        counts.put("testRunStatusCounts", testRunStatusCounts(testRuns));
        return counts;
    }

    private Map<String, Object> reportCounts(DynamicEvidenceJoinResult joinResult, List<?> testRuns) {
        Map<String, Object> counts = baseStatusCounts(joinResult);
        Map<String, Integer> statusCounts = testRunStatusCounts(testRuns);
        int selected = testRuns == null ? 0 : testRuns.size();
        counts.put("testClassesSelected", selected);
        counts.put("testClassesPassed", statusCounts.getOrDefault("PASSED", 0));
        counts.put("testClassesFailed", statusCounts.getOrDefault("FAILED", 0));
        counts.put("testClassesTimedOut", statusCounts.getOrDefault("TIMED_OUT", 0));
        counts.put("testClassesSkipped", statusCounts.getOrDefault("SKIPPED", 0));
        counts.put("testClassesNoReport", statusCounts.getOrDefault("NO_REPORT", 0));
        counts.put("evidenceFilesRead", joinResult.evidenceFilesRead());
        counts.put("evidenceBytesRead", joinResult.evidenceBytesRead());
        counts.put("dynamicEventsRead", joinResult.dynamicEventsRead());
        counts.put("eventsMissingTestContext", joinResult.eventsMissingTestContext());
        counts.put("workloadPlansRead", joinResult.records().size());
        counts.put("workloadPlansEnriched", joinResult.records().size());
        return counts;
    }

    private Map<String, Object> baseStatusCounts(DynamicEvidenceJoinResult joinResult) {
        Map<DynamicEvidenceJoinStatus, Integer> enumCounts = new EnumMap<>(DynamicEvidenceJoinStatus.class);
        for (DynamicEvidenceJoinStatus status : DynamicEvidenceJoinStatus.values()) {
            enumCounts.put(status, 0);
        }
        joinResult.records().forEach(record -> enumCounts.compute(record.dynamicEvidence().joinStatus(), (status, count) -> count == null ? 1 : count + 1));
        Map<String, Object> counts = new LinkedHashMap<>();
        for (DynamicEvidenceJoinStatus status : DynamicEvidenceJoinStatus.values()) {
            counts.put(status.name(), enumCounts.get(status));
        }
        counts.put("unmatchedReasonCounts", unmatchedReasonCounts(joinResult));
        return counts;
    }

    private Map<String, Integer> unmatchedReasonCounts(DynamicEvidenceJoinResult joinResult) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (UnmatchedReason reason : UnmatchedReason.values()) {
            counts.put(reason.name(), 0);
        }
        joinResult.records().stream()
                .filter(record -> record.dynamicEvidence().joinStatus() == DynamicEvidenceJoinStatus.UNMATCHED)
                .map(record -> record.dynamicEvidence().unmatchedReason())
                .forEach(reason -> counts.merge((reason == null ? UnmatchedReason.UNCLASSIFIED : reason).name(), 1, Integer::sum));
        return counts;
    }

    private Map<String, Integer> testRunStatusCounts(List<?> testRuns) {
        Map<String, Integer> counts = new LinkedHashMap<>();
        if (testRuns == null) {
            return counts;
        }
        for (Object run : testRuns) {
            String status = statusOf(run);
            if (status != null) {
                counts.merge(status, 1, Integer::sum);
            }
        }
        return counts;
    }

    private String runStatus(List<?> testRuns) {
        if (testRuns == null || testRuns.isEmpty()) {
            return "COMPLETE";
        }
        List<String> statuses = new ArrayList<>();
        for (Object run : testRuns) {
            statuses.add(statusOf(run));
        }
        if (statuses.stream().anyMatch("FAILED"::equals)
                || statuses.stream().anyMatch("TIMED_OUT"::equals)
                || statuses.stream().anyMatch("NO_REPORT"::equals)) {
            return "PARTIAL";
        }
        return "COMPLETE";
    }

    @SuppressWarnings("unchecked")
    private String statusOf(Object run) {
        if (run instanceof Map<?, ?> map) {
            Object status = map.get("status");
            return status == null ? null : status.toString();
        }
        return null;
    }

    private void createParent(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }
}
