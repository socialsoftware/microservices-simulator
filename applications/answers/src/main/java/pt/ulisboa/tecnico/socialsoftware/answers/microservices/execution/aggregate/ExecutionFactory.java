package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

public interface ExecutionFactory {
    Execution createExecution(Integer aggregateId,  Dto);
    Execution createExecutionFromExisting(Execution existingExecution);
     createExecutionDto(Execution );
}
