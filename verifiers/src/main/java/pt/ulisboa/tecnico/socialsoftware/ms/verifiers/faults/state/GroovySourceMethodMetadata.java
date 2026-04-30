package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovySourceMethodMetadata(
        String name,
        String returnTypeName,
        List<GroovySourceAnnotationMetadata> annotations,
        List<String> constructedTypeNames
) {
    public GroovySourceMethodMetadata {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        constructedTypeNames = constructedTypeNames == null ? List.of() : List.copyOf(constructedTypeNames);
    }
}
