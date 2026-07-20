package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public record StepAnalysisDiagnostic(
        DispatchPhase phase,
        String code,
        String message) {
}
