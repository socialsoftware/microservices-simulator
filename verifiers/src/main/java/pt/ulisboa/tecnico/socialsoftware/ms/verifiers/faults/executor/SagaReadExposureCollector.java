package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import org.springframework.core.env.Environment;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseAdapter;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.SagaReadExposureReport.*;

/** Attempt-local metadata accumulator, composed into the existing write observer in production. */
public final class SagaReadExposureCollector implements ImpactEvidenceObserver {
    public static final String ENABLED_PROPERTY = "microservices.simulator.saga-read-exposure.enabled";
    private final String attemptId;
    private final String workloadPlanId;
    private final String faultScenarioId;
    private final SourceContract sources;
    private final List<Revision> baseline = new ArrayList<>();
    private final List<Write> writes = new ArrayList<>();
    private final List<Call> calls = new ArrayList<>();
    private final List<Gap> gaps = new ArrayList<>();
    private List<ReadResponseEvidence.Contract> contracts = List.of();
    private long order;
    private boolean started;
    private boolean baselineAbsenceCovered;
    private String unavailableReason = "MEASUREMENT_NOT_STARTED";

    public SagaReadExposureCollector(String attemptId, String workloadPlanId, String faultScenarioId,
                                     SourceContract sources) {
        this.attemptId = attemptId;
        this.workloadPlanId = workloadPlanId;
        this.faultScenarioId = faultScenarioId;
        this.sources = sources == null ? new SourceContract(List.of(), List.of()) : sources;
    }

    static boolean enabled(ScenarioRuntimeContext runtime) {
        try {
            // Same Environment as LocalCommandGateway's @Value; JVM fallback is for non-Spring fixtures.
            Object bean = runtime.bean(Environment.class);
            if (bean instanceof Environment environment) return Boolean.parseBoolean(
                    environment.getProperty(ENABLED_PROPERTY, "false"));
        } catch (RuntimeException ignored) { }
        return Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "false"));
    }

    static List<ReadResponseEvidence.Contract> contracts(ScenarioRuntimeContext runtime) {
        return runtime.beans(ReadResponseAdapter.class).stream().map(adapter ->
                new ReadResponseEvidence.Contract(adapter.contractId(), adapter.contractVersion(),
                        adapter.commandType().getName(), adapter.responseType().getName(),
                        adapter.aggregateType(), adapter.runtimeType())).toList();
    }

    /** Reuses the one baseline queried by ImpactV2. No persistence access occurs here. */
    public synchronized void begin(ImpactEvidence.SnapshotBatch snapshot,
                                    List<ReadResponseEvidence.Contract> declaredContracts) {
        if (started) throw new IllegalStateException("Diagnostic measurement already started");
        contracts = copy(declaredContracts).stream().sorted(Comparator.comparing(
                ReadResponseEvidence.Contract::id, Comparator.nullsFirst(String::compareTo))
                .thenComparing(ReadResponseEvidence.Contract::version, Comparator.nullsFirst(String::compareTo))
                .thenComparing(ReadResponseEvidence.Contract::commandType, Comparator.nullsFirst(String::compareTo))
                .thenComparing(ReadResponseEvidence.Contract::responseType, Comparator.nullsFirst(String::compareTo)))
                .toList();
        baseline.addAll(snapshot.aggregates().stream().map(Revision::from).toList());
        snapshot.gaps().forEach(gap -> gaps.add(new Gap(0, "BASELINE", gap.subject(), gap.reason())));
        baselineAbsenceCovered = snapshot.gaps().isEmpty();
        started = true;
        unavailableReason = null;
    }

    public synchronized void unavailable(String reason) { unavailableReason = reason; }
    @Override public synchronized boolean isEnabled() { return started && unavailableReason == null; }
    @Override public synchronized boolean isReadObservationEnabled() { return isEnabled(); }

    @Override public synchronized void committedWrite(ImpactEvidence.AggregateSnapshot aggregate,
                                                       ImpactEvidence.Writer writer) {
        if (!started) return;
        long position = ++order;
        writes.add(new Write(attemptId + ":write:" + position, position, Revision.from(aggregate), writer));
    }

    @Override public synchronized void readResponse(ReadResponseEvidence.Observation observation) {
        if (!started) return;
        long position = ++order;
        calls.add(new Call(attemptId + ":call:" + position, position, observation));
    }

    /** Event facts are owned exclusively by ImpactV2 and do not consume this ordering. */
    @Override public void eventDelivery(ImpactEvidence.EventDelivery ignored) { }

    @Override public synchronized void coverageGap(ImpactEvidence.CoverageGap gap) {
        gaps.add(new Gap(order, gap.stage(), gap.subject(), gap.reason()));
    }

    synchronized void retainedWriteFailure(ImpactEvidence.CoverageGap gap) {
        // Holder failures drained at the end have no trustworthy occurrence position.
        gaps.add(new Gap(0, gap.stage(), gap.subject(), gap.reason()));
    }

    public synchronized void readFailure(ImpactEvidence.CoverageGap gap) {
        gaps.add(new Gap(order, "READ_OBSERVATION", gap.subject(), gap.reason()));
    }

    synchronized void failure(String reason) { gaps.add(new Gap(order, "DIAGNOSTIC", null, reason)); }

    public synchronized SagaReadExposureReport report(ScenarioExecutionReport execution,
                                                       List<ArtifactReference> artifacts) {
        try {
            return new SagaReadExposureAssessor().assess(execution, attemptId, workloadPlanId, faultScenarioId,
                    started, unavailableReason, baselineAbsenceCovered, contracts, baseline, sources,
                    writes, calls, gaps, artifacts);
        } catch (RuntimeException failure) {
            List<Gap> failedGaps = new ArrayList<>(gaps);
            failedGaps.add(new Gap(order, "DIAGNOSTIC", null, "ASSESSMENT_FAILED"));
            return new SagaReadExposureReport(null, attemptId, workloadPlanId, faultScenarioId,
                    execution.terminalStatus(), execution.scheduleConformance(), "UNDETERMINED", "UNAVAILABLE",
                    "ASSESSMENT_FAILED", null, SCOPE, EXCLUDED_PATHS, contracts, artifacts,
                    baselineAbsenceCovered, baseline, sources, List.of(), writes, calls, List.of(), List.of(), failedGaps);
        }
    }
}
