package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovySourceFieldMetadata(
        String name,
        String typeName,
        List<GroovySourceAnnotationMetadata> annotations
) {
    public GroovySourceFieldMetadata {
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
    }
}
