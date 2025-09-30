package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

public record CommandResponse(
        String correlationId,
        boolean isError,
        String errorMessage,
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        Object result
) {
    public static CommandResponse success(String correlationId, Object result) {
        return new CommandResponse(correlationId, false, null, result);
    }

    public static CommandResponse error(String correlationId, String errorMessage) {
        return new CommandResponse(correlationId, true, errorMessage, null);
    }
}
