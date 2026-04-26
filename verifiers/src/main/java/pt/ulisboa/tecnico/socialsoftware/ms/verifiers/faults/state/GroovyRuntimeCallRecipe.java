package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyRuntimeCallRecipe(
        String receiverText,
        String methodName,
        List<GroovyRuntimeCallArgument> arguments,
        String sourceText) {

    public GroovyRuntimeCallRecipe {
        arguments = arguments == null ? List.of() : List.copyOf(arguments);
    }
}
