package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyTraceArgument(
        int index,
        String provenance,
        GroovyValueRecipe recipe,
        String expectedTypeFqn,
        GroovySourceValueReference producerReference) {

    public GroovyTraceArgument(int index, String provenance, GroovyValueRecipe recipe) {
        this(index, provenance, recipe, deriveExpectedTypeFqn(recipe), null);
    }

    public GroovyTraceArgument(int index,
                               String provenance,
                               GroovyValueRecipe recipe,
                               GroovySourceValueReference producerReference) {
        this(index, provenance, recipe, deriveExpectedTypeFqn(recipe), producerReference);
    }

    public GroovyTraceArgument(int index,
                               String provenance,
                               GroovyValueRecipe recipe,
                               String expectedTypeFqn) {
        this(index, provenance, recipe, expectedTypeFqn, null);
    }

    private static String deriveExpectedTypeFqn(GroovyValueRecipe recipe) {
        if (recipe == null) {
            return null;
        }

        GroovyValueMetadata metadata = recipe.metadata();
        return metadata == null ? null : metadata.expectedTypeFqn();
    }
}
