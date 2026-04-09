package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public record StepDispatchFootprint(
        String stepKey,
        String commandTypeFqn,
        String aggregateName,
        AccessPolicy accessPolicy,
        DispatchPhase phase,
        DispatchMultiplicity multiplicity) {
}
