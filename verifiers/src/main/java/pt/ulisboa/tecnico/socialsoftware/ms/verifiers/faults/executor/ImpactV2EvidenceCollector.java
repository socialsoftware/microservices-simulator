package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.PersistentStateObserver;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class ImpactV2EvidenceCollector implements ImpactEvidenceObserver {
    static final String ENABLED_PROPERTY = "microservices.simulator.impact.enabled";
    private final String attemptId;
    private final String workloadPlanId;
    private final String faultScenarioId;
    private final ImpactV2Assessor assessor;
    private final AtomicLong sequence = new AtomicLong();
    private final List<ImpactEvidence.CommittedWrite> writes = new ArrayList<>();
    private final List<ImpactEvidence.EventDelivery> deliveries = new ArrayList<>();
    private final List<ImpactEvidence.CoverageGap> gaps = new ArrayList<>();
    private List<ImpactEvidence.AggregateSnapshot> baseline = List.of();
    private List<ImpactEvidence.AggregateSnapshot> finalState = List.of();
    private PersistentStateObserver stateObserver;
    private String collectionStatus = "UNAVAILABLE";
    private String collectionReason = "MEASUREMENT_NOT_STARTED";

    ImpactV2EvidenceCollector(String attemptId, String workloadPlanId, String faultScenarioId) {
        this(attemptId, workloadPlanId, faultScenarioId, new ImpactV2Assessor());
    }

    ImpactV2EvidenceCollector(String attemptId, String workloadPlanId, String faultScenarioId,
                              ImpactV2Assessor assessor) {
        this.attemptId = attemptId;
        this.workloadPlanId = workloadPlanId;
        this.faultScenarioId = faultScenarioId;
        this.assessor = assessor;
    }

    void start(ScenarioRuntimeContext runtimeContext) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"))) {
            collectionStatus = "UNAVAILABLE";
            collectionReason = "COLLECTION_DISABLED";
            return;
        }
        try {
            stateObserver = (PersistentStateObserver) runtimeContext.bean(PersistentStateObserver.class);
            ImpactEvidence.SnapshotBatch snapshot = stateObserver.snapshotAll();
            baseline = snapshot.aggregates();
            gaps.addAll(snapshot.gaps());
            collectionStatus = gaps.isEmpty() ? "OBSERVED" : "PARTIAL";
            collectionReason = gaps.isEmpty() ? null : "BASELINE_COVERAGE_GAPS";
        } catch (RuntimeException failure) {
            stateObserver = null;
            collectionStatus = "UNAVAILABLE";
            collectionReason = "OBSERVER_UNAVAILABLE";
            gaps.add(gap("BASELINE", "observer", "OBSERVER_UNAVAILABLE", failure));
        }
    }

    synchronized void finish() {
        if (stateObserver == null) return;
        try {
            ImpactEvidence.SnapshotBatch snapshot = stateObserver.snapshotAll();
            finalState = snapshot.aggregates();
            gaps.addAll(snapshot.gaps());
        } catch (RuntimeException failure) {
            collectionStatus = "PARTIAL";
            collectionReason = "FINAL_SNAPSHOT_FAILED";
            gaps.add(gap("FINAL", "observer", "FINAL_SNAPSHOT_FAILED", failure));
        }
        List<ImpactEvidence.EventDelivery> horizonDeliveries = new ArrayList<>();
        for (ImpactEvidence.EventDelivery delivery : List.copyOf(deliveries)) {
            try {
                PersistentStateObserver.HorizonObservation observation = stateObserver.observeAtHorizon(delivery);
                horizonDeliveries.add(observation.delivery());
                gaps.addAll(observation.gaps());
            } catch (RuntimeException failure) {
                horizonDeliveries.add(delivery);
                gaps.add(gap("HORIZON_EVENT", String.valueOf(delivery.eventId()),
                        "HORIZON_EVENT_OBSERVATION_FAILED", failure));
            }
        }
        deliveries.clear();
        deliveries.addAll(horizonDeliveries);
        if (!gaps.isEmpty()) {
            collectionStatus = "PARTIAL";
            if (collectionReason == null) collectionReason = "COVERAGE_GAPS";
        }
    }

    boolean started() { return stateObserver != null; }

    void recordObserverFailures(ImpactEvidenceObserverHolder.Scope scope) {
        scope.drainFailures().forEach(this::coverageGap);
    }

    @Override public synchronized void committedWrite(ImpactEvidence.AggregateSnapshot aggregate,
                                                       ImpactEvidence.Writer writer) {
        writes.add(new ImpactEvidence.CommittedWrite(sequence.incrementAndGet(), aggregate, writer));
        if (!ownedWriter(writer)) {
            gaps.add(new ImpactEvidence.CoverageGap("WRITE", aggregate.identity().toString(),
                    "WRITER_IDENTITY_UNAVAILABLE",
                    "committed write has missing or mismatched attempt/workload attribution"));
            collectionStatus = "PARTIAL";
            collectionReason = "COVERAGE_GAPS";
        }
    }

    @Override public synchronized void eventDelivery(ImpactEvidence.EventDelivery delivery) {
        deliveries.add(new ImpactEvidence.EventDelivery(sequence.incrementAndGet(), delivery.eventId(),
                delivery.eventType(), delivery.publisherAggregateId(), delivery.publisherAggregateVersion(),
                delivery.receiverBefore(), delivery.receiverAfter(), delivery.eligibleBefore(),
                delivery.eligibleAfter(), delivery.receiverFinal(), delivery.eligibleAtHorizon(), delivery.writer()));
        if (!ownedWriter(delivery.writer())) {
            gaps.add(new ImpactEvidence.CoverageGap("EVENT_DELIVERY", String.valueOf(delivery.eventId()),
                    "WRITER_IDENTITY_UNAVAILABLE",
                    "event consumer has missing or mismatched attempt/workload attribution"));
            collectionStatus = "PARTIAL";
            collectionReason = "COVERAGE_GAPS";
        }
    }

    @Override public synchronized void coverageGap(ImpactEvidence.CoverageGap gap) {
        gaps.add(gap);
        collectionStatus = "PARTIAL";
        collectionReason = "COVERAGE_GAPS";
    }

    ImpactV2EvidenceReport report(ScenarioExecutionReport execution) {
        synchronized (this) {
            try {
                return assessor.assess(execution, attemptId, workloadPlanId, faultScenarioId,
                        collectionStatus, collectionReason, baseline, finalState, writes, deliveries, gaps);
            } catch (RuntimeException failure) {
                gaps.add(gap("ASSESSMENT", attemptId, "ASSESSMENT_FAILED", failure));
                return ImpactV2Assessor.assessmentFailure(execution, attemptId, workloadPlanId, faultScenarioId,
                        collectionStatus, collectionReason, baseline, finalState, writes, deliveries, gaps);
            }
        }
    }

    private boolean ownedWriter(ImpactEvidence.Writer writer) {
        return writer != null && !"UNKNOWN".equals(writer.kind())
                && attemptId.equals(writer.executionAttemptId())
                && java.util.Objects.equals(workloadPlanId, writer.workloadPlanId());
    }

    private ImpactEvidence.CoverageGap gap(String stage, String subject, String reason, Throwable failure) {
        return new ImpactEvidence.CoverageGap(stage, subject, reason,
                failure.getClass().getName() + ": " + failure.getMessage());
    }
}
