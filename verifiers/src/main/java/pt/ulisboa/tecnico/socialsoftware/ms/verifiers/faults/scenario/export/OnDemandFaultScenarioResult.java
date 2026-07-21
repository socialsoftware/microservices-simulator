package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import java.util.List;

public record OnDemandFaultScenarioResult(
        Status status,
        String workloadPlanId,
        String assignedVector,
        Integer recoveryScheduleCap,
        String uncappedScheduleCount,
        int writtenScheduleCount,
        int addedFaultScenarioCount,
        List<String> faultScenarioIds,
        List<Diagnostic> diagnostics) {

    public OnDemandFaultScenarioResult {
        faultScenarioIds = faultScenarioIds == null ? List.of() : List.copyOf(faultScenarioIds);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    public boolean successful() {
        return status == Status.PERSISTED || status == Status.DEDUPLICATED;
    }

    public enum Status {
        PERSISTED,
        DEDUPLICATED,
        REJECTED,
        INTEGRITY_FAILURE,
        PERSISTENCE_FAILED
    }

    public record Diagnostic(String code, String message) {
    }
}
