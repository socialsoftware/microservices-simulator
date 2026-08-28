package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

/** Links one Saga constructor aggregate-key position to its source producer. */
public record SourceAggregateKeyInputEvidence(
        String sagaFqn,
        String sourceClassFqn,
        String sourceMethodName,
        String callContextMethodName,
        String sourceBindingName,
        int constructorArgumentIndex,
        String aggregateName,
        GroovySourceValueReference producerReference) {
}
