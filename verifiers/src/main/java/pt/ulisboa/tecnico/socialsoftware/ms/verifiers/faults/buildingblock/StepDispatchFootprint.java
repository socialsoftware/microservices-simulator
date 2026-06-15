package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public record StepDispatchFootprint(
        String stepKey,
        String commandTypeFqn,
        String aggregateName,
        AccessPolicy accessPolicy,
        DispatchPhase phase,
        DispatchMultiplicity multiplicity,
        String aggregateKeyText,
        AggregateKeyConfidence aggregateKeyConfidence) {

    public StepDispatchFootprint(String stepKey,
                                 String commandTypeFqn,
                                 String aggregateName,
                                 AccessPolicy accessPolicy,
                                 DispatchPhase phase,
                                 DispatchMultiplicity multiplicity) {
        this(stepKey, commandTypeFqn, aggregateName, accessPolicy, phase, multiplicity, null, null);
    }

    public StepDispatchFootprint {
        aggregateKeyConfidence = aggregateKeyText == null || aggregateKeyText.isBlank()
                ? null
                : aggregateKeyConfidence == null ? AggregateKeyConfidence.SYMBOLIC : aggregateKeyConfidence;
    }

    public enum AggregateKeyConfidence {
        EXACT,
        SYMBOLIC
    }
}
