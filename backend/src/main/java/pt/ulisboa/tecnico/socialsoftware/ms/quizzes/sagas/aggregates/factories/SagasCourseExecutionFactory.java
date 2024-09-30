package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaCourseExecution;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.dtos.SagaCourseExecutionDto;

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

    @Override
    public CourseExecutionDto createCourseExecutionDto(CourseExecution courseExecution) {
        return new SagaCourseExecutionDto(courseExecution);
    }
}
