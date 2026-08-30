package pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle;

/** A semantic-lock acquisition deliberately skipped during an oracle run. */
public record IgnoredSemanticLockAcquisition(
        SemanticLockId semanticLock,
        Integer aggregateId,
        String functionalityName,
        String stepName) {
}
