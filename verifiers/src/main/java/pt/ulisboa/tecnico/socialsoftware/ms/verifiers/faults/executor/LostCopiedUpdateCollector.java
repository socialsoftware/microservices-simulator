package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.springframework.core.env.Environment;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateObservation;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.copiedupdate.CopiedUpdateSession;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;

/** Composes copied-input observation with the existing attempt baseline and committed writes. */
final class LostCopiedUpdateCollector {
    static final String ENABLED_PROPERTY = "microservices.simulator.lost-copied-update.enabled";
    private final String attemptId;
    private final Path contractsPath;
    private List<ImpactEvidence.AggregateSnapshot> baseline = List.of();
    private CopiedUpdateSession session;
    private Map<String, Object> trace =
            Map.of(
                    "events",
                    List.of(),
                    "contracts",
                    List.of(),
                    "gaps",
                    List.of("OBSERVATION_NOT_STARTED"));
    private boolean started;
    private String manifestHash;
    private final List<String> lateGaps = new ArrayList<>();

    LostCopiedUpdateCollector(String attemptId, Path packagePath) {
        this.attemptId = attemptId;
        packagePath = packagePath.toAbsolutePath().normalize();
        this.contractsPath =
                (Files.isDirectory(packagePath) ? packagePath : packagePath.getParent())
                        .resolve("copy-contracts.json");
    }

    static boolean enabled(ScenarioRuntimeContext runtime) {
        try {
            Object bean = runtime.bean(Environment.class);
            if (bean instanceof Environment environment)
                return Boolean.parseBoolean(environment.getProperty(ENABLED_PROPERTY, "false"));
        } catch (RuntimeException ignored) {
        }
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }

    void begin(ImpactEvidence.SnapshotBatch snapshot) {
        baseline = snapshot.aggregates();
        try {
            manifestHash =
                    HexFormat.of()
                            .formatHex(
                                    MessageDigest.getInstance("SHA-256")
                                            .digest(Files.readAllBytes(contractsPath)));
            session = CopiedUpdateObservation.start(attemptId, manifestHash);
            started = session.isRecording();
            snapshot.gaps().forEach(gap -> session.gap("BASELINE:" + gap.reason()));
        } catch (Exception | LinkageError failure) {
            trace =
                    Map.of(
                            "events",
                            List.of(),
                            "contracts",
                            List.of(),
                            "gaps",
                            List.of("OBSERVER_START_FAILED:" + failure.getClass().getSimpleName()));
        }
    }

    void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer) {
        if (session != null) session.committed(aggregate, writer);
    }

    void gap(String reason) {
        if (session != null) session.gap(reason);
        else lateGaps.add(reason);
    }

    void finish() {
        if (session != null) {
            trace = CopiedUpdateObservation.finish(session);
            session = null;
        }
    }

    Map<String, Object> report(ScenarioExecutionReport execution) {
        boolean valid =
                started
                        && Set.of("SUCCESS", "COMPENSATED", "PARTIAL_COMPENSATED")
                                .contains(execution.terminalStatus())
                        && Set.of("EXACT", "DEVIATED")
                                .contains(String.valueOf(execution.scheduleConformance()));
        if (!lateGaps.isEmpty()) {
            var allGaps = new ArrayList<Object>((List<?>) trace.getOrDefault("gaps", List.of()));
            allGaps.addAll(lateGaps);
            trace = new LinkedHashMap<>(trace);
            trace.put("gaps", allGaps);
        }
        var result = new LinkedHashMap<String, Object>();
        result.put("schemaVersion", "lost-copied-updates.v1");
        result.put("executionAttemptId", execution.executionAttemptId());
        result.put("workloadPlanId", execution.workloadPlanId());
        result.put("faultScenarioId", execution.faultScenarioId());
        result.put("executionTerminalStatus", execution.terminalStatus());
        result.put("scheduleConformance", execution.scheduleConformance());
        result.put("validity", valid ? "COMPLETE" : "UNAVAILABLE");
        result.put("contractsSha256", manifestHash);
        result.put("baseline", baseline);
        result.put("evidence", trace);
        try {
            var assessment = new LostCopiedUpdateAssessor().assess(trace, baseline);
            result.put(
                    "coverage",
                    !started
                            ? "UNAVAILABLE"
                            : assessment.coverageGaps().isEmpty()
                                    ? "COMPLETE_WITHIN_SCOPE"
                                    : "INCOMPLETE");
            result.put("count", assessment.findings().size());
            result.put("findings", assessment.findings());
            result.put("coverageGaps", assessment.coverageGaps());
            result.put("scope", assessment.scope());
        } catch (RuntimeException failure) {
            result.put("coverage", "UNAVAILABLE");
            result.put("count", 0);
            result.put("findings", List.of());
            result.put(
                    "coverageGaps",
                    List.of("ASSESSMENT_FAILED:" + failure.getClass().getSimpleName()));
        }
        return result;
    }
}
