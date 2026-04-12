package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

public record GroovyFullTraceResult(
        String sourceClassFqn,
        String sourceMethodName,
        String sagaClassFqn,
        String traceText) {
}
