package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record SetupPlan(
        String schemaVersion,
        List<SetupAction> actions,
        List<SetupParticipantBinding> participantBindings,
        List<String> blockers) {

    public static final String SCHEMA_VERSION = "microservices-simulator.setup-plan.v1";

    public SetupPlan {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion;
        actions = actions == null ? List.of() : List.copyOf(actions);
        participantBindings = participantBindings == null ? List.of() : List.copyOf(participantBindings);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
