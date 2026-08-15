package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record NormalActionRef(
        int normalOrder,
        NormalActionKind kind,
        String scheduledStepId,
        String eventConsequenceId) {

    public NormalActionRef {
        scheduledStepId = normalize(scheduledStepId);
        eventConsequenceId = normalize(eventConsequenceId);
    }

    public static NormalActionRef forward(int normalOrder, String scheduledStepId) {
        return new NormalActionRef(normalOrder, NormalActionKind.FORWARD, scheduledStepId, null);
    }

    public static NormalActionRef eventConsequence(int normalOrder, String eventConsequenceId) {
        return new NormalActionRef(normalOrder, NormalActionKind.EVENT_CONSEQUENCE, null, eventConsequenceId);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
