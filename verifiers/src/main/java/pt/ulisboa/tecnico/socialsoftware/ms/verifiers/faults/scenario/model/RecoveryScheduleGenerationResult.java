package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.math.BigInteger;
import java.util.List;

public record RecoveryScheduleGenerationResult(
        List<FaultScenario> faultScenarios,
        BigInteger uncappedScheduleCount,
        int writtenScheduleCount,
        int recoveryScheduleCap,
        List<FaultSlotGenerationDiagnostic> faultSlotDiagnostics,
        RecoveryScheduleGenerationMetrics metrics) {

    public RecoveryScheduleGenerationResult {
        faultScenarios = faultScenarios == null ? List.of() : List.copyOf(faultScenarios);
        uncappedScheduleCount = uncappedScheduleCount == null ? BigInteger.ZERO : uncappedScheduleCount;
        faultSlotDiagnostics = faultSlotDiagnostics == null ? List.of() : List.copyOf(faultSlotDiagnostics);
        metrics = metrics == null ? new RecoveryScheduleGenerationMetrics(0, 0, 0) : metrics;
    }
}
