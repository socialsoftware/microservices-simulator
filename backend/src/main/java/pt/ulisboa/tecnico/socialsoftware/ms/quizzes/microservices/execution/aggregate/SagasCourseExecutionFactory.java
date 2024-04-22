package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;

@Service
@Profile("sagas")
public class SagasCourseExecutionFactory implements CourseExecutionFactory {
    @Override
    public CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse) {
        return new SagaCourseExecution(aggregateId, courseExecutionDto, courseExecutionCourse);
    }

    @Override
    public CourseExecution createCourseExecutionFromExisting(CourseExecution existingAnswer) {
        return new SagaCourseExecution((SagaCourseExecution) existingAnswer);
    }
}
