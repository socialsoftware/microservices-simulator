package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

/** Connects one exact pair of accepted source inputs to its extracted fixture setup. */
public record SourceSetupPlanBinding(
        String leftInputVariantId,
        String rightInputVariantId,
        SetupPlan setupPlan) {
}
