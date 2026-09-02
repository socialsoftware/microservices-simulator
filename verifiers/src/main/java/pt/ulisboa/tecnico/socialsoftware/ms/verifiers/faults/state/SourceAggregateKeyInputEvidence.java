package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

/** Links one Saga constructor aggregate-key position to its source producer. */
public record SourceAggregateKeyInputEvidence(
        String sagaFqn,
        String sourceClassFqn,
        String sourceMethodName,
        String callContextMethodName,
        String sourceBindingName,
        int constructorArgumentIndex,
        String aggregateName,
        GroovySourceValueReference producerReference,
        List<String> aggregateKeyPropertyPath,
        String inputVariantId) {

    public SourceAggregateKeyInputEvidence(String sagaFqn,
                                           String sourceClassFqn,
                                           String sourceMethodName,
                                           String callContextMethodName,
                                           String sourceBindingName,
                                           int constructorArgumentIndex,
                                           String aggregateName,
                                           GroovySourceValueReference producerReference) {
        this(sagaFqn, sourceClassFqn, sourceMethodName, callContextMethodName,
                sourceBindingName, constructorArgumentIndex, aggregateName,
                producerReference, List.of(), null);
    }

    public SourceAggregateKeyInputEvidence {
        aggregateKeyPropertyPath = aggregateKeyPropertyPath == null
                ? List.of() : List.copyOf(aggregateKeyPropertyPath);
    }
}
