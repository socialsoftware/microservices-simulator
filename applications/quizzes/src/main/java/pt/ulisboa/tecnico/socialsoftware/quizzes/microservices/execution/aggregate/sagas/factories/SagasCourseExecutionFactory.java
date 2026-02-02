package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.Execution;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.sagas.SagaExecution;

@Service
@Profile("sagas")
public class SagasCourseExecutionFactory implements CourseExecutionFactory {
    @Override
    public Execution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        return new SagaExecution(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    @Override
    public Execution createCourseExecutionFromExisting(Execution existingAnswer) {
        return new SagaExecution((SagaExecution) existingAnswer);
    }

    @Override
    public CourseExecutionDto createCourseExecutionDto(Execution execution) {
        return new CourseExecutionDto(execution);
    }
}
