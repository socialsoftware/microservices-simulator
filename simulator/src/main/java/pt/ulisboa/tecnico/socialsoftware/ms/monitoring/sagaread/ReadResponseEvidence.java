package pt.ulisboa.tecnico.socialsoftware.ms.monitoring.sagaread;

import pt.ulisboa.tecnico.socialsoftware.ms.monitoring.impact.ImpactEvidence;

/** Metadata-only facts. Sequence numbers are assigned by the composed attempt collector. */
public final class ReadResponseEvidence {
    private ReadResponseEvidence() { }

    public record Contract(String id, String version, String commandType, String responseType,
                           String aggregateType, String runtimeType) { }

    public enum Outcome { DELIVERED, DELIVERED_UNMAPPED, DELIVERED_INVALID, FAILED, FAILED_INVALID, EXCLUDED }

    public record Observation(
            ImpactEvidence.Writer reader,
            String transportCommandType,
            String commandType,
            String responseType,
            boolean serialized,
            Outcome outcome,
            Contract contract,
            ImpactEvidence.AggregateIdentity identity,
            Long version,
            String reason) { }
}
