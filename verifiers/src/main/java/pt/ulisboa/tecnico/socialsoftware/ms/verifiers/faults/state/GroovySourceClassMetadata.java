package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.nio.file.Path;
import java.util.List;

public record GroovySourceClassMetadata(
        Path sourceFile,
        String packageName,
        List<GroovyImportMetadata> imports,
        String declaredSuperclassName
) {}
