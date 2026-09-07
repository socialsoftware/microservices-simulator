package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;

/** Offline reassessment of retained observations; never executes application actions. */
public class Reassess {
    public static void main(String[] args) throws Exception {
        var mapper = new ObjectMapper().findAndRegisterModules()
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        Path output = Path.of(args[1]);
        Files.createDirectory(output);
        var rows = new ArrayList<Object>();
        for (var entry : mapper.readTree(Path.of(args[0]).toFile())) {
            String name = entry.path("name").asText();
            if (!name.matches("[a-zA-Z0-9_-]+")) throw new IllegalArgumentException("Invalid case name");
            Path executionPath = Path.of(entry.path("execution").asText());
            Path impactPath = Path.of(entry.path("impact").asText());
            var execution = mapper.readValue(executionPath.toFile(), ScenarioExecutionReport.class);
            var before = mapper.readValue(impactPath.toFile(), ImpactV2EvidenceReport.class);
            if (!execution.executionAttemptId().equals(before.executionAttemptId())
                    || !execution.workloadPlanId().equals(before.workloadPlanId())
                    || !execution.faultScenarioId().equals(before.faultScenarioId())) {
                throw new IllegalArgumentException("Mismatched reports: " + name);
            }
            var after = new ImpactV2Assessor().assess(execution, before.executionAttemptId(),
                    before.workloadPlanId(), before.faultScenarioId(), before.collectionStatus(),
                    before.collectionReason(), before.baseline(), before.finalState(),
                    before.committedWrites(), before.eventDeliveries(), before.coverageGaps());
            Path destination = output.resolve(name + ".impact-v2.json");
            mapper.writerWithDefaultPrettyPrinter().writeValue(destination.toFile(), after);
            var row = new LinkedHashMap<String, Object>();
            row.put("name", name);
            row.put("execution", executionPath.toString());
            row.put("executionSha256", hash(executionPath));
            row.put("originalImpact", impactPath.toString());
            row.put("originalImpactSha256", hash(impactPath));
            row.put("executionAttemptId", before.executionAttemptId());
            row.put("oldScore", before.completeScore());
            row.put("newScore", after.completeScore());
            row.put("oldStatus", before.assessmentStatus());
            row.put("newStatus", after.assessmentStatus());
            row.put("oldCategories", before.categoryResults());
            row.put("newCategories", after.categoryResults());
            row.put("reassessmentSha256", hash(destination));
            rows.add(row);
        }
        var summary = new LinkedHashMap<String, Object>();
        summary.put("policy", "recovered-creation-remnant-exclusion-2026-09-07");
        summary.put("mode", "OFFLINE_REASSESSMENT_NO_NEW_APPLICATION_EXECUTIONS");
        summary.put("inputManifestSha256", hash(Path.of(args[0])));
        summary.put("rows", rows);
        mapper.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(), summary);
    }

    private static String hash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
}
