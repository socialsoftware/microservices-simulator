package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

public record EventConsequence(
        String deterministicId,
        String triggerScheduledStepId,
        EventEmissionSite emissionSite,
        String eventTypeFqn,
        String eventHandlingClassFqn,
        String eventHandlingMethodName,
        String eventHandlerClassFqn,
        String eventProcessingClassFqn,
        String eventProcessingMethodName,
        String facadeClassFqn,
        String facadeMethodName,
        String downstreamSagaFqn,
        String deliveryPolicy,
        List<String> diagnostics) {

    public EventConsequence {
        deterministicId = normalize(deterministicId);
        triggerScheduledStepId = normalize(triggerScheduledStepId);
        eventTypeFqn = normalize(eventTypeFqn);
        eventHandlingClassFqn = normalize(eventHandlingClassFqn);
        eventHandlingMethodName = normalize(eventHandlingMethodName);
        eventHandlerClassFqn = normalize(eventHandlerClassFqn);
        eventProcessingClassFqn = normalize(eventProcessingClassFqn);
        eventProcessingMethodName = normalize(eventProcessingMethodName);
        facadeClassFqn = normalize(facadeClassFqn);
        facadeMethodName = normalize(facadeMethodName);
        downstreamSagaFqn = normalize(downstreamSagaFqn);
        deliveryPolicy = normalize(deliveryPolicy);
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }

    private static String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
