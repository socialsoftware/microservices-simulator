package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;
import java.util.Objects;

public record InputRecipe(
        String schemaVersion,
        String recipeFingerprint,
        boolean executorReady,
        List<String> blockers,
        List<InputRecipeArgument> arguments) {

    public static final String SCHEMA_VERSION = "microservices-simulator.input-recipe.v1";

    public InputRecipe {
        schemaVersion = schemaVersion == null || schemaVersion.isBlank() ? SCHEMA_VERSION : schemaVersion.trim();
        blockers = InputRecipeCollections.stableStrings(blockers);
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        recipeFingerprint = recipeFingerprint == null || recipeFingerprint.isBlank()
                ? InputRecipeFingerprinter.fingerprint(schemaVersion, executorReady, blockers, arguments)
                : recipeFingerprint.trim();
    }

    public String semanticFingerprint() {
        return InputRecipeFingerprinter.fingerprint(schemaVersion, executorReady, blockers, arguments);
    }

    public boolean fingerprintMatchesSemanticContent() {
        return Objects.equals(recipeFingerprint, semanticFingerprint());
    }
}
