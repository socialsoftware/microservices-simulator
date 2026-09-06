package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact;

public interface ImpactEvidenceObserver {
    default boolean isEnabled() {
        return true;
    }

    void committedWrite(ImpactEvidence.AggregateSnapshot aggregate, ImpactEvidence.Writer writer);

    void eventDelivery(ImpactEvidence.EventDelivery delivery);

    void coverageGap(ImpactEvidence.CoverageGap gap);
}
