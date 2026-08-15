package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

public record BaselineBindingRequirement(String key, String typeFqn) {
    public BaselineBindingRequirement {
        key = normalize(key);
        typeFqn = normalize(typeFqn);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
