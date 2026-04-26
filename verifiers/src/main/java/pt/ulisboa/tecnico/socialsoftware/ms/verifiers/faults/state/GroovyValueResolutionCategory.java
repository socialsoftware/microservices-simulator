package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public enum GroovyValueResolutionCategory {
    RESOLVED,
    SOURCE_PLACEHOLDER,
    INJECTABLE_PLACEHOLDER,
    RUNTIME_CALL,
    UNKNOWN_UNRESOLVED
}
