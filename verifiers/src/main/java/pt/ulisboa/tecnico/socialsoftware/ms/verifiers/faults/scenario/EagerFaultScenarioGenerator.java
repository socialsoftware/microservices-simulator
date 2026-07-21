package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario;

import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor.ScenarioExecutorReadinessEvaluator;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.ComputedVectorRecovery;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.EagerFaultScenarioGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenario;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.FaultScenarioVectorSource;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.InputVariant;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.RecoveryScheduleGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.SagaInstance;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadGenerationResult;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadMaterializability;
import pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model.WorkloadPlan;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class EagerFaultScenarioGenerator {

    private EagerFaultScenarioGenerator() {
    }

    public static EagerFaultScenarioGenerationResult generate(WorkloadGenerationResult workloadResult,
                                                               RecoveryScheduleCap recoveryScheduleCap) {
        WorkloadGenerationResult source = Objects.requireNonNull(workloadResult, "workloadResult");
        RecoveryScheduleCap cap = Objects.requireNonNull(recoveryScheduleCap, "recoveryScheduleCap");
        List<WorkloadPlan> orderedPlans = source.workloadPlans().stream()
                .sorted(Comparator.comparing(WorkloadPlan::kind)
                        .thenComparing(WorkloadPlan::deterministicId, Comparator.nullsFirst(String::compareTo)))
                .toList();
        WorkloadGenerationResult orderedWorkloads = new WorkloadGenerationResult(
                source.schemaVersion(),
                source.effectiveConfig(),
                orderedPlans,
                source.rejectedInputVariants(),
                source.counts(),
                source.warnings());

        List<WorkloadMaterializability> materializability = new ArrayList<>();
        List<ComputedVectorRecovery> computedVectors = new ArrayList<>();
        LinkedHashMap<String, FaultScenario> faultScenariosById = new LinkedHashMap<>();

        for (WorkloadPlan plan : orderedPlans) {
            WorkloadMaterializability eligibility = evaluateMaterializability(plan);
            materializability.add(eligibility);
            if (!eligibility.materializable()) {
                continue;
            }

            int slotCount = plan.faultSlots().size();
            generateVector(plan, "0".repeat(slotCount), FaultScenarioVectorSource.EAGER_ALL_ZERO,
                    cap, computedVectors, faultScenariosById);
            for (int slotIndex = 0; slotIndex < slotCount; slotIndex++) {
                char[] vector = "0".repeat(slotCount).toCharArray();
                vector[slotIndex] = '1';
                generateVector(plan, new String(vector), FaultScenarioVectorSource.EAGER_SINGLE_POINT,
                        cap, computedVectors, faultScenariosById);
            }
        }

        return new EagerFaultScenarioGenerationResult(
                orderedWorkloads,
                cap.value(),
                List.copyOf(faultScenariosById.values()),
                materializability,
                computedVectors);
    }

    public static WorkloadMaterializability evaluateMaterializability(WorkloadPlan plan) {
        List<String> diagnostics = new ArrayList<>();
        WorkloadPlanValidator.ValidationResult structural = new WorkloadPlanValidator().validate(plan);
        structural.diagnostics().forEach(diagnostic -> diagnostics.add(
                "STRUCTURAL:" + diagnostic.code() + ":" + diagnostic.message()));

        Map<String, InputVariant> inputsById = new LinkedHashMap<>();
        plan.acceptedInputs().forEach(input -> {
            if (input != null && input.deterministicId() != null) {
                inputsById.putIfAbsent(input.deterministicId(), input);
            }
        });
        ScenarioExecutorReadinessEvaluator readinessEvaluator = new ScenarioExecutorReadinessEvaluator();
        for (SagaInstance participant : plan.participants()) {
            if (participant == null) {
                continue;
            }
            InputVariant input = inputsById.get(participant.inputVariantId());
            if (input == null) {
                continue;
            }
            ScenarioExecutorReadinessEvaluator.Readiness readiness = readinessEvaluator.evaluate(input);
            if (!readiness.materializable()) {
                if (readiness.blockers().isEmpty()) {
                    diagnostics.add("INPUT:" + participant.deterministicId() + ":NOT_MATERIALIZABLE");
                } else {
                    readiness.blockers().forEach(blocker -> diagnostics.add(
                            "INPUT:" + participant.deterministicId() + ":" + input.deterministicId() + ":" + blocker));
                }
            }
        }
        return new WorkloadMaterializability(plan.deterministicId(), diagnostics.isEmpty(), diagnostics);
    }

    private static void generateVector(WorkloadPlan plan,
                                       String vector,
                                       FaultScenarioVectorSource source,
                                       RecoveryScheduleCap cap,
                                       List<ComputedVectorRecovery> computedVectors,
                                       LinkedHashMap<String, FaultScenario> faultScenariosById) {
        RecoveryScheduleGenerationResult generated = RecoveryScheduleGenerator.generate(plan, vector, cap);
        if (source == FaultScenarioVectorSource.EAGER_ALL_ZERO && generated.writtenScheduleCount() != 1) {
            throw new IllegalStateException("all-zero eager generation must produce exactly one FaultScenario");
        }
        computedVectors.add(new ComputedVectorRecovery(
                plan.deterministicId(),
                vector,
                source,
                generated.uncappedScheduleCount(),
                generated.writtenScheduleCount()));
        for (FaultScenario scenario : generated.faultScenarios()) {
            FaultScenario existing = faultScenariosById.putIfAbsent(scenario.deterministicId(), scenario);
            if (existing != null && !existing.equals(scenario)) {
                throw new IllegalStateException("FaultScenario id collision for " + scenario.deterministicId());
            }
        }
    }
}
