package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

public interface ExecutionFactory {
    Execution createExecution(Integer aggregateId, ExecutionDto executionDto);
    Execution createExecutionFromExisting(Execution existingExecution);
    ExecutionDto createExecutionDto(Execution execution);
}
