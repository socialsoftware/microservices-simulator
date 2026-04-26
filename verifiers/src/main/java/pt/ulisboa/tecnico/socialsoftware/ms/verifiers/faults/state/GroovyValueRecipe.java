package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyValueRecipe(
        GroovyValueKind kind,
        String text,
        List<GroovyValueRecipe> children,
        GroovyValueMetadata metadata) {

    public GroovyValueRecipe(GroovyValueKind kind, String text, List<GroovyValueRecipe> children) {
        this(kind, text, children, GroovyValueMetadata.defaultMetadata());
    }

    public GroovyValueRecipe {
        children = children == null ? List.of() : List.copyOf(children);
        metadata = metadata == null ? GroovyValueMetadata.defaultMetadata() : metadata;
    }
}
