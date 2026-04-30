package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.nio.file.Path;
import java.util.List;

public record GroovySourceClassMetadata(
        Path sourceFile,
        String packageName,
        List<GroovyImportMetadata> imports,
        String declaredSuperclassName,
        List<GroovySourceAnnotationMetadata> annotations,
        List<GroovySourceFieldMetadata> fields,
        List<GroovySourceMethodMetadata> methods,
        String enclosingClassFqn,
        boolean staticClass
) {
    public GroovySourceClassMetadata {
        imports = imports == null ? List.of() : List.copyOf(imports);
        annotations = annotations == null ? List.of() : List.copyOf(annotations);
        fields = fields == null ? List.of() : List.copyOf(fields);
        methods = methods == null ? List.of() : List.copyOf(methods);
    }
}
