package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread.ReadResponseEvidence;

public interface ImpactEvidenceObserver {
    default boolean isEnabled() {
        return true;
    }

    void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer);

    void eventDelivery(ImpactEvidence.EventDelivery delivery);

    void coverageGap(ImpactEvidence.CoverageGap gap);

    /** Independently opt-in: read collection must never enable the write observer. */
    default boolean isReadObservationEnabled() { return false; }

    /** Composed into the same attempt scope, but not into ImpactV2 evidence or scores. */
    default void readResponse(ReadResponseEvidence.Observation observation) { }
}
