package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyConstructorInputTrace(
        String sourceClassFqn,
        String sourceMethodName,
        String sourceBindingName,
        String sagaClassFqn) {
}
