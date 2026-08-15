package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.executor;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ScenarioPrerequisiteResult(
        Map<String, Object> bindings,
        Map<String, String> evidence) {

    public ScenarioPrerequisiteResult {
        bindings = bindings == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(bindings));
        evidence = evidence == null ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(evidence));
    }
}
