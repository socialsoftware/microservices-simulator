package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

/** One source-ordered, Java-confirmed facade call recovered from a fixture/helper path. */
public record GroovyFacadeSetupActionTrace(
        String sourceClassFqn,
        String callContextMethodName,
        String sourceOccurrence,
        String facadeClassFqn,
        String methodKey,
        String methodName,
        List<GroovyTraceArgument> arguments,
        String declaredResultTypeFqn,
        boolean voidResult,
        List<String> blockers) {

    public GroovyFacadeSetupActionTrace {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
        blockers = blockers == null ? List.of() : List.copyOf(blockers);
    }
}
