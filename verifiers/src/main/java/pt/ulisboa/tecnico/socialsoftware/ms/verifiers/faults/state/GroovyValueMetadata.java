package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyValueMetadata(
        GroovyValueResolutionCategory category,
        String expectedTypeFqn,
        String placeholderId,
        GroovyRuntimeCallRecipe runtimeCall,
        List<GroovyAssignmentRecipe> assignments) {

    public GroovyValueMetadata {
        category = category == null ? GroovyValueResolutionCategory.RESOLVED : category;
        assignments = assignments == null ? List.of() : List.copyOf(assignments);
    }

    public GroovyValueMetadata(GroovyValueResolutionCategory category,
                               String expectedTypeFqn,
                               String placeholderId,
                               GroovyRuntimeCallRecipe runtimeCall) {
        this(category, expectedTypeFqn, placeholderId, runtimeCall, List.of());
    }

    public static GroovyValueMetadata defaultMetadata() {
        return new GroovyValueMetadata(GroovyValueResolutionCategory.RESOLVED, null, null, null, List.of());
    }
}
