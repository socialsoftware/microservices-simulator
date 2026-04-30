package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.state;

import java.util.List;

public record GroovyFullTraceResult(
        String sourceClassFqn,
        String sourceMethodName,
        String sourceBindingName,
        GroovyTraceOriginKind originKind,
        String sourceExpressionText,
        String sagaClassFqn,
        SourceMode sourceMode,
        SourceModeConfidence sourceModeConfidence,
        List<String> sourceModeEvidence,
        List<GroovyTraceArgument> constructorArguments,
        List<GroovyWorkflowCall> workflowCalls,
        List<String> resolutionNotes,
        String traceText) {

    public GroovyFullTraceResult {
        sourceMode = sourceMode == null ? SourceMode.UNKNOWN : sourceMode;
        sourceModeConfidence = sourceModeConfidence == null ? SourceModeConfidence.UNKNOWN : sourceModeConfidence;
        sourceModeEvidence = sourceModeEvidence == null ? List.of() : List.copyOf(sourceModeEvidence);
        constructorArguments = constructorArguments == null ? List.of() : List.copyOf(constructorArguments);
        workflowCalls = workflowCalls == null ? List.of() : List.copyOf(workflowCalls);
        resolutionNotes = resolutionNotes == null ? List.of() : List.copyOf(resolutionNotes);
    }

    public GroovyFullTraceResult(String sourceClassFqn,
                                 String sourceMethodName,
                                 String sourceBindingName,
                                 GroovyTraceOriginKind originKind,
                                 String sourceExpressionText,
                                 String sagaClassFqn,
                                 List<GroovyTraceArgument> constructorArguments,
                                 List<GroovyWorkflowCall> workflowCalls,
                                 List<String> resolutionNotes,
                                 String traceText) {
        this(sourceClassFqn,
                sourceMethodName,
                sourceBindingName,
                originKind,
                sourceExpressionText,
                sagaClassFqn,
                SourceMode.UNKNOWN,
                SourceModeConfidence.UNKNOWN,
                List.of(),
                constructorArguments,
                workflowCalls,
                resolutionNotes,
                traceText);
    }
}
