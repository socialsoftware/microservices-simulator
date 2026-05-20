package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record InputRecipeArgument(
        int index,
        String expectedTypeFqn,
        InputResolutionStatus resolutionStatus,
        boolean executorReady,
        List<String> blockers,
        String provenanceText,
        InputRecipeNode recipe) {

    public InputRecipeArgument {
        expectedTypeFqn = normalize(expectedTypeFqn);
        resolutionStatus = resolutionStatus == null ? InputResolutionStatus.UNRESOLVED : resolutionStatus;
        blockers = InputRecipeCollections.stableStrings(blockers);
        provenanceText = normalize(provenanceText);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
