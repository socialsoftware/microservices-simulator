package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyAssignmentRecipe(
        String assignmentKind,
        String propertyName,
        String sourceName,
        int orderIndex,
        String sourceText,
        GroovyValueRecipe valueRecipe,
        String blocker) {
}
