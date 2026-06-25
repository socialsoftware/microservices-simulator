package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.buildingblock;

public record EventDrivenArgumentSource(
        int argumentIndex,
        EventDrivenArgumentSourceKind kind,
        String provenance,
        String recipeText,
        String placeholderId,
        String eventFieldName) {
}
