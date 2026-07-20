package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.export;

import java.nio.file.Path;

public record OnDemandFaultScenarioRequest(
        Path manifestPath,
        String workloadPlanId,
        String assignedVector,
        String assertedRecoveryScheduleCap) {
}
