package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyImportMetadata(
        String rawText,
        String importedType,
        String alias,
        boolean star,
        boolean staticImport
) {}
