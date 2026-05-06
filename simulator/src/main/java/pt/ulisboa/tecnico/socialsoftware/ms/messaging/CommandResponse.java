package pt.ulisboa.tecnico.socialsoftware.ms.messaging;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;

public record CommandResponse(
        String correlationId,
        boolean isError,
        String errorType,
        String errorMessage,
        String errorTemplate,
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        Object result,
        UnitOfWork unitOfWork
) {
    public static CommandResponse success(String correlationId, Object result, UnitOfWork unitOfWork) {
        return new CommandResponse(correlationId, false, null, null, null, result, unitOfWork);
    }

    public static CommandResponse error(String correlationId, Exception error, UnitOfWork unitOfWork) {
        String errorTemplate = error instanceof SimulatorException
                ? ((SimulatorException) error).getErrorMessage()
                : null;
        return new CommandResponse(correlationId, true,
                error.getClass().getName(), error.getMessage(), errorTemplate, null, unitOfWork);
    }
}
