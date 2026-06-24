package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecution;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate.sagas.SagaCourseExecution;

@Service
@Profile("sagas")
public class SagasCourseExecutionFactory implements CourseExecutionFactory {

    @Override
    public CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto dto, CourseExecutionCourse courseExecutionCourse) {
        return new SagaCourseExecution(aggregateId, dto, courseExecutionCourse);
    }

    @Override
    public CourseExecution createCourseExecutionFromExisting(CourseExecution existing) {
        return new SagaCourseExecution((SagaCourseExecution) existing);
    }

    @Override
    public CourseExecutionDto createCourseExecutionDto(CourseExecution courseExecution) {
        return new CourseExecutionDto(courseExecution);
    }
}
