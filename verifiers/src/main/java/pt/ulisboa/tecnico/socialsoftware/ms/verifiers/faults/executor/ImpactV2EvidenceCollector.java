package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidenceObserverHolder;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.PersistentStateObserver;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadObservationContext;
import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

final class ImpactV2EvidenceCollector implements ImpactEvidenceObserver {
    static final String ENABLED_PROPERTY = "microservices.simulator.impact.enabled";
    private final String attemptId;
    private final String workloadPlanId;
    private final String faultScenarioId;
    private final ImpactV2Assessor assessor;
    private final SagaReadExposureCollector sagaReads;
    private LostCopiedUpdateCollector copiedUpdates;
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
        this(attemptId, workloadPlanId, faultScenarioId, assessor, null);
    }

    ImpactV2EvidenceCollector(String attemptId, String workloadPlanId, String faultScenarioId,
                              SagaReadExposureCollector sagaReads) {
        this(attemptId, workloadPlanId, faultScenarioId, new ImpactV2Assessor(), sagaReads);
    }

    private ImpactV2EvidenceCollector(String attemptId, String workloadPlanId, String faultScenarioId,
                                      ImpactV2Assessor assessor, SagaReadExposureCollector sagaReads) {
        this.attemptId = attemptId;
        this.workloadPlanId = workloadPlanId;
        this.faultScenarioId = faultScenarioId;
        this.assessor = assessor;
        this.sagaReads = sagaReads;
    }

    void copiedUpdates(LostCopiedUpdateCollector collector) { this.copiedUpdates = collector; }

    private void copied(java.util.function.Consumer<LostCopiedUpdateCollector> operation) {
        if (copiedUpdates == null) return;
        try { operation.accept(copiedUpdates); }
        catch (RuntimeException failure) { copiedUpdates.gap("OBSERVER_CALLBACK_FAILED"); }
    }

    void start(ScenarioRuntimeContext runtimeContext) {
        if (!Boolean.parseBoolean(System.getProperty(ENABLED_PROPERTY, "true"))) {
            collectionStatus = "UNAVAILABLE";
            collectionReason = "COLLECTION_DISABLED";
            diagnostic(value -> value.unavailable("WRITE_COLLECTION_DISABLED"));
            return;
        }
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER")) {
            stateObserver = (PersistentStateObserver) runtimeContext.bean(PersistentStateObserver.class);
            ImpactEvidence.SnapshotBatch snapshot = stateObserver.snapshotAll();
            baseline = snapshot.aggregates();
            gaps.addAll(snapshot.gaps());
            collectionStatus = gaps.isEmpty() ? "OBSERVED" : "PARTIAL";
            collectionReason = gaps.isEmpty() ? null : "BASELINE_COVERAGE_GAPS";
            diagnostic(value -> value.begin(snapshot, SagaReadExposureCollector.contracts(runtimeContext)));
            copied(value -> value.begin(snapshot));
        } catch (RuntimeException failure) {
            stateObserver = null;
            collectionStatus = "UNAVAILABLE";
            collectionReason = "OBSERVER_UNAVAILABLE";
            gaps.add(gap("BASELINE", "observer", "OBSERVER_UNAVAILABLE", failure));
            diagnostic(value -> value.unavailable("WRITE_OBSERVER_UNAVAILABLE"));
        }
    }

    synchronized void finish() {
        copied(LostCopiedUpdateCollector::finish);
        if (stateObserver == null) return;
        try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER")) {
            ImpactEvidence.SnapshotBatch snapshot = stateObserver.snapshotAll();
            finalState = snapshot.aggregates();
            gaps.addAll(snapshot.gaps());
            snapshot.gaps().forEach(gap -> diagnostic(value -> value.coverageGap(gap)));
        } catch (RuntimeException failure) {
            collectionStatus = "PARTIAL";
            collectionReason = "FINAL_SNAPSHOT_FAILED";
            gaps.add(gap("FINAL", "observer", "FINAL_SNAPSHOT_FAILED", failure));
            diagnostic(value -> value.coverageGap(gap("FINAL", "observer", "FINAL_SNAPSHOT_FAILED", failure)));
        }
        List<ImpactEvidence.EventDelivery> horizonDeliveries = new ArrayList<>();
        for (ImpactEvidence.EventDelivery delivery : List.copyOf(deliveries)) {
            try (ReadObservationContext.Scope ignored = ReadObservationContext.exclude("OBSERVER")) {
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

    void observationUnavailable(String reason) {
        diagnostic(value -> value.unavailable(reason));
        copied(value -> value.gap(reason));
    }

    synchronized void recordObserverFailures(ImpactEvidenceObserverHolder.Scope scope) {
        scope.drainFailures().forEach(gap -> {
            gaps.add(gap);
            collectionStatus = "PARTIAL";
            collectionReason = "COVERAGE_GAPS";
            diagnostic(value -> value.retainedWriteFailure(gap));
            copied(value -> value.gap(gap.reason()));
        });
        scope.drainReadFailures().forEach(gap -> diagnostic(value -> value.readFailure(gap)));
    }

    @Override public synchronized void committedWrite(ImpactEvidence.AggregateSnapshot aggregate,
                                                       ImpactEvidence.Writer writer) {
        writes.add(new ImpactEvidence.CommittedWrite(sequence.incrementAndGet(), aggregate, writer));
        diagnostic(value -> value.committedWrite(aggregate, writer));
        copied(value -> value.committedWrite(aggregate, writer));
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
        diagnostic(value -> value.coverageGap(gap));
        copied(value -> value.gap(gap.reason()));
        collectionStatus = "PARTIAL";
        collectionReason = "COVERAGE_GAPS";
    }

    @Override public boolean isReadObservationEnabled() {
        return sagaReads != null && started() && sagaReads.isReadObservationEnabled();
    }

    @Override public void readResponse(ReadResponseEvidence.Observation observation) {
        if (sagaReads != null) sagaReads.readResponse(observation);
    }

    private void diagnostic(java.util.function.Consumer<SagaReadExposureCollector> operation) {
        if (sagaReads == null) return;
        try {
            operation.accept(sagaReads);
        } catch (RuntimeException failure) {
            sagaReads.failure("DIAGNOSTIC_COLLECTION_FAILED");
        }
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
