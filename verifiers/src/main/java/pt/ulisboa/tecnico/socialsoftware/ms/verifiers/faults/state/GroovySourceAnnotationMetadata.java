package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.LinkedHashMap;
import java.util.Map;

public record GroovySourceAnnotationMetadata(
        String name,
        Map<String, Object> attributes
) {
    public GroovySourceAnnotationMetadata {
        attributes = attributes == null ? Map.of() : java.util.Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
    }
}
