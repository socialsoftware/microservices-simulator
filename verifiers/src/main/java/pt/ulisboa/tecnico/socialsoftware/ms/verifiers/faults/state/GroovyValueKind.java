package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public enum GroovyValueKind {
    UNRESOLVED_VARIABLE,
    UNRESOLVED_RUNTIME_EDGE,
    LITERAL,
    CONSTRUCTOR,
    HELPER_CALL_RESULT,
    PROPERTY_ACCESS,
    COLLECTION_LITERAL,
    LOCAL_TRANSFORM
}
