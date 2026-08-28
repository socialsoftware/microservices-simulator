package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyValueRecipe(
        GroovyValueKind kind,
        String text,
        List<GroovyValueRecipe> children,
        GroovyValueMetadata metadata,
        GroovySourceValueReference sourceReference) {

    public GroovyValueRecipe(GroovyValueKind kind, String text, List<GroovyValueRecipe> children) {
        this(kind, text, children, GroovyValueMetadata.defaultMetadata(), null);
    }

    public GroovyValueRecipe(GroovyValueKind kind, String text, List<GroovyValueRecipe> children,
                             GroovyValueMetadata metadata) {
        this(kind, text, children, metadata, null);
    }

    public GroovyValueRecipe {
        children = children == null ? List.of() : List.copyOf(children);
        metadata = metadata == null ? GroovyValueMetadata.defaultMetadata() : metadata;
    }
}
