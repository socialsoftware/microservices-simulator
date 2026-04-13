package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyFullTraceResult(
        String sourceClassFqn,
        String sourceMethodName,
        String sourceBindingName,
        GroovyTraceOriginKind originKind,
        String sourceExpressionText,
        String sagaClassFqn,
        List<GroovyTraceArgument> constructorArguments,
        List<GroovyWorkflowCall> workflowCalls,
        List<String> resolutionNotes,
        String traceText) {
}
