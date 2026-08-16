package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.SagaExecution;

@Service
@Profile("sagas")
public class SagasExecutionFactory implements ExecutionFactory {

    @Override
    public SagaExecution createExecution(Integer aggregateId, String acronym, String academicTerm, ExecutionCourse executionCourse) {
        return new SagaExecution(aggregateId, acronym, academicTerm, executionCourse);
    }

    @Override
    public SagaExecution createExecutionCopy(Execution existing) {
        return new SagaExecution((SagaExecution) existing);
    }

    @Override
    public ExecutionDto createExecutionDto(Execution execution) {
        return new ExecutionDto(execution);
    }
}
