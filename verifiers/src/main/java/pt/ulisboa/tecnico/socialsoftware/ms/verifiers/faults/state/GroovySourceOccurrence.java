package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

/** Typed source identity and caller-relative order for one traced Groovy call. */
public record GroovySourceOccurrence(
        String sourceClassFqn,
        String callContextMethodName,
        String occurrenceId,
        int orderIndex,
        boolean initialPreparationPhase) {

    public GroovySourceOccurrence(String sourceClassFqn,
                                  String callContextMethodName,
                                  String occurrenceId,
                                  int orderIndex) {
        this(sourceClassFqn, callContextMethodName, occurrenceId, orderIndex, true);
    }
}
