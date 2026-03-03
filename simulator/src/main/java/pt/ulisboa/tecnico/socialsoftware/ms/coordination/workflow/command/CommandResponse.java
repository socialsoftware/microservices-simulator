package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public record CommandResponse(
        String correlationId,
        boolean isError,
        String errorType,
        String errorMessage,
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        Object result,
        UnitOfWork unitOfWork
) {
    public static CommandResponse success(String correlationId, Object result, UnitOfWork unitOfWork) {
        return new CommandResponse(correlationId, false, null, null, result, unitOfWork);
    }

    public static CommandResponse error(String correlationId, Exception error, UnitOfWork unitOfWork) {
        return new CommandResponse(correlationId, true,
                error.getClass().getName(), error.getMessage(), null, unitOfWork);
    }
}
