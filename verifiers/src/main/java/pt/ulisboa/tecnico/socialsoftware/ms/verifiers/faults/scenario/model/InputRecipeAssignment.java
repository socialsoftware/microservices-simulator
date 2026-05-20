package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record InputRecipeAssignment(
        String assignmentKind,
        String propertyName,
        String sourceName,
        int orderIndex,
        String sourceText,
        boolean executorReady,
        List<String> blockers,
        InputRecipeNode valueRecipe) {

    public InputRecipeAssignment {
        assignmentKind = normalize(assignmentKind);
        propertyName = normalize(propertyName);
        sourceName = normalize(sourceName);
        sourceText = normalize(sourceText);
        blockers = InputRecipeCollections.stableStrings(blockers);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
