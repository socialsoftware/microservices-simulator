package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.SagaExecution;

import java.time.LocalDateTime;

@Service
@Profile("sagas")
public class SagasExecutionFactory implements ExecutionFactory {
    @Override
    public SagaExecution createExecution(Integer aggregateId, Integer courseAggregateId, String courseName,
                                         CourseType courseType, String acronym, String academicTerm,
                                         LocalDateTime endDate) {
        return new SagaExecution(aggregateId, courseAggregateId, courseName, courseType, acronym, academicTerm,
                endDate);
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
