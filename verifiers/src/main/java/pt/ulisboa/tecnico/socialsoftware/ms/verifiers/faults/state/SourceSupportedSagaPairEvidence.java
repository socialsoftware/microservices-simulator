package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

/** Strong source/test support for two Saga inputs that consume one producer property. */
public record SourceSupportedSagaPairEvidence(
        String aggregateName,
        SourceAggregateKeyInputEvidence left,
        SourceAggregateKeyInputEvidence right) {
}
