package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.Comparator;
import java.util.List;

public record PrerequisiteBaseline(
        String providerId,
        String providerVersion,
        List<BaselineBindingRequirement> requiredBindings) {

    public PrerequisiteBaseline {
        providerId = normalize(providerId);
        providerVersion = normalize(providerVersion);
        requiredBindings = requiredBindings == null ? List.of() : requiredBindings.stream()
                .sorted(Comparator.comparing(BaselineBindingRequirement::key,
                        Comparator.nullsFirst(String::compareTo)))
                .toList();
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
