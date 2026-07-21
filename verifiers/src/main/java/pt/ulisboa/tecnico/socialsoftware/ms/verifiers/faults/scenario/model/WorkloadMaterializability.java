package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record WorkloadMaterializability(
        String workloadPlanId,
        boolean materializable,
        List<String> diagnostics) {

    public WorkloadMaterializability {
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
