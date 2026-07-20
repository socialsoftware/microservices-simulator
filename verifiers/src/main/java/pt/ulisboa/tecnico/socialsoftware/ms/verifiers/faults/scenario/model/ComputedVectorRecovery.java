package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.math.BigInteger;

public record ComputedVectorRecovery(
        String workloadPlanId,
        String assignedVector,
        FaultScenarioVectorSource vectorSource,
        BigInteger uncappedScheduleCount,
        int writtenScheduleCount) {

    public ComputedVectorRecovery {
        uncappedScheduleCount = uncappedScheduleCount == null ? BigInteger.ZERO : uncappedScheduleCount;
        if (writtenScheduleCount < 0) {
            throw new IllegalArgumentException("written schedule count must be non-negative");
        }
    }
}
