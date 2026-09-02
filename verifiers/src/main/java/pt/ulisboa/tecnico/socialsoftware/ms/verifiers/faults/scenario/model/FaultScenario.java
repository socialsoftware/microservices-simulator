package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record FaultScenario(
        String schemaVersion,
        String deterministicId,
        String workloadPlanId,
        String assignedVector,
        List<FaultScenarioAction> actions) {

    public static final String SCHEMA_VERSION = "microservices-simulator.fault-scenario.v4";
    /** Compact current-package projection schema. */
    public static final String CURRENT_SCHEMA_VERSION = "microservices-simulator.fault-scenario.current";

    public FaultScenario {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        deterministicId = normalize(deterministicId);
        workloadPlanId = normalize(workloadPlanId);
        actions = actions == null ? List.of() : List.copyOf(actions);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
