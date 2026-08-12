package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.SagaExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.sagas.dtos.SagaExecutionDto;

@Service
@Profile("sagas")
public class SagasExecutionFactory implements ExecutionFactory {
    @Override
    public Execution createExecution(Integer aggregateId, ExecutionDto executionDto) {
        return new SagaExecution(aggregateId, executionDto);
    }

    @Override
    public Execution createExecutionFromExisting(Execution existingExecution) {
        return new SagaExecution((SagaExecution) existingExecution);
    }

    @Override
    public ExecutionDto createExecutionDto(Execution execution) {
        return new SagaExecutionDto(execution);
    }
}