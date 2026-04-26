package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyValueMetadata(
        GroovyValueResolutionCategory category,
        String expectedTypeFqn,
        String placeholderId,
        GroovyRuntimeCallRecipe runtimeCall) {

    public GroovyValueMetadata {
        category = category == null ? GroovyValueResolutionCategory.RESOLVED : category;
    }

    public static GroovyValueMetadata defaultMetadata() {
        return new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, null, null, null);
    }
}
