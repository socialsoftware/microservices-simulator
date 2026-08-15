package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.scenario.model;

import java.util.List;

/** Static producer/consumer join before a concrete scheduled trigger occurrence exists. */
public record EventConsequenceDefinition(
        String triggerSagaFqn,
        String triggerStepKey,
        EventEmissionSite emissionSite,
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

    public static final String UNIQUE_MATCHING_SUBSCRIBER = "UNIQUE_MATCHING_SUBSCRIBER";

    public EventConsequenceDefinition {
        deliveryPolicy = deliveryPolicy == null || deliveryPolicy.isBlank()
                ? UNIQUE_MATCHING_SUBSCRIBER
                : deliveryPolicy.trim();
        diagnostics = diagnostics == null ? List.of() : List.copyOf(diagnostics);
    }
}
