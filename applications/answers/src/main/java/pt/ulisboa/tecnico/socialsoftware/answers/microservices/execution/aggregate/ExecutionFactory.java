package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

public interface ExecutionFactory {
    Execution createExecution(Integer aggregateId, ExecutionCourse course, ExecutionDto executionDto, Set<ExecutionUser> users);
    Execution createExecutionFromExisting(Execution existingExecution);
    ExecutionDto createExecutionDto(Execution execution);
}
