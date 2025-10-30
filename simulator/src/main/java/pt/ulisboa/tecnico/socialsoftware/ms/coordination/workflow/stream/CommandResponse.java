package pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.stream;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.internal.builders.JUnit3Builder;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;

public record CommandResponse(
        String correlationId,
        boolean isError,
        String errorMessage,
        @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
        Object result,
        UnitOfWork unitOfWork
) {
    public static CommandResponse success(String correlationId, Object result, UnitOfWork unitOfWork) {
        return new CommandResponse(correlationId, false, null, result, unitOfWork);
    }

    public static CommandResponse error(String correlationId, String errorMessage, UnitOfWork unitOfWork) {
        return new CommandResponse(correlationId, true, errorMessage, null, unitOfWork);
    }
}
