package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.ScenarioGeneratorConfig;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public record EagerFaultScenarioGenerationResult(
        WorkloadGenerationResult workloadGenerationResult,
        int recoveryScheduleCap,
        List<FaultScenario> faultScenarios,
        List<WorkloadMaterializability> workloadMaterializability,
        List<ComputedVectorRecovery> computedVectors) {

    public EagerFaultScenarioGenerationResult {
        workloadGenerationResult = Objects.requireNonNull(workloadGenerationResult, "workloadGenerationResult");
        if (recoveryScheduleCap <= 0) {
            throw new IllegalArgumentException("recovery schedule cap must be a positive integer");
        }
        faultScenarios = faultScenarios == null ? List.of() : List.copyOf(faultScenarios);
        workloadMaterializability = workloadMaterializability == null ? List.of() : List.copyOf(workloadMaterializability);
        computedVectors = computedVectors == null ? List.of() : List.copyOf(computedVectors);
    }

    public String schemaVersion() {
        return workloadGenerationResult.schemaVersion();
    }

    public ScenarioGeneratorConfig effectiveConfig() {
        return workloadGenerationResult.effectiveConfig();
    }

    public List<WorkloadPlan> workloadPlans() {
        return workloadGenerationResult.workloadPlans();
    }

    public List<RejectedInputVariant> rejectedInputVariants() {
        return workloadGenerationResult.rejectedInputVariants();
    }

    public Map<String, Integer> counts() {
        return workloadGenerationResult.counts();
    }

    public List<String> warnings() {
        return workloadGenerationResult.warnings();
    }
}
