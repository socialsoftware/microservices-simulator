package pt.ulisboa.tecnico.socialsoftware.ms.verifiers.faults.dynamic.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonPropertyOrder({"testExecution", "saga", "sagaInvocation", "status", "observationIds", "input",
        "candidateInputs", "reason"})
public record DynamicAttributionLink(
        String testExecution,
        String saga,
        String sagaInvocation,
        String status,
        List<String> observationIds,
        String input,
        List<String> candidateInputs,
        String reason) {
    public DynamicAttributionLink {
        observationIds = observationIds == null ? List.of() : List.copyOf(observationIds);
        candidateInputs = candidateInputs == null ? List.of() : List.copyOf(candidateInputs);
    }
}
