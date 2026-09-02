package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

import java.util.List;

public record StepDispatchFootprint(
        String stepKey,
        String commandTypeFqn,
        String aggregateName,
        AccessPolicy accessPolicy,
        DispatchPhase phase,
        DispatchMultiplicity multiplicity,
        String aggregateKeyText,
        AggregateKeyConfidence aggregateKeyConfidence,
        Integer aggregateKeyConstructorArgumentIndex,
        List<String> aggregateKeyPropertyPath) {

    public StepDispatchFootprint(String stepKey,
                                 String commandTypeFqn,
                                 String aggregateName,
                                 AccessPolicy accessPolicy,
                                 DispatchPhase phase,
                                 DispatchMultiplicity multiplicity) {
        this(stepKey, commandTypeFqn, aggregateName, accessPolicy, phase, multiplicity,
                null, null, null, List.of());
    }

    public StepDispatchFootprint(String stepKey,
                                 String commandTypeFqn,
                                 String aggregateName,
                                 AccessPolicy accessPolicy,
                                 DispatchPhase phase,
                                 DispatchMultiplicity multiplicity,
                                 String aggregateKeyText,
                                 AggregateKeyConfidence aggregateKeyConfidence) {
        this(stepKey, commandTypeFqn, aggregateName, accessPolicy, phase, multiplicity,
                aggregateKeyText, aggregateKeyConfidence, null, List.of());
    }

    public StepDispatchFootprint(String stepKey,
                                 String commandTypeFqn,
                                 String aggregateName,
                                 AccessPolicy accessPolicy,
                                 DispatchPhase phase,
                                 DispatchMultiplicity multiplicity,
                                 String aggregateKeyText,
                                 AggregateKeyConfidence aggregateKeyConfidence,
                                 Integer aggregateKeyConstructorArgumentIndex) {
        this(stepKey, commandTypeFqn, aggregateName, accessPolicy, phase, multiplicity,
                aggregateKeyText, aggregateKeyConfidence, aggregateKeyConstructorArgumentIndex, List.of());
    }

    public StepDispatchFootprint {
        aggregateKeyConfidence = aggregateKeyText == null || aggregateKeyText.isBlank()
                ? null
                : aggregateKeyConfidence == null ? AggregateKeyConfidence.SYMBOLIC : aggregateKeyConfidence;
        aggregateKeyPropertyPath = aggregateKeyPropertyPath == null
                ? List.of() : List.copyOf(aggregateKeyPropertyPath);
    }

    public enum AggregateKeyConfidence {
        EXACT,
        SYMBOLIC
    }
}
