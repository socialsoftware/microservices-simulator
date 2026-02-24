package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate.sagas.SagaCourse;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

@Service
@Profile("sagas")
public class SagasCourseFactory implements CourseFactory {
    @Override
    public Course createCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto) {
        return new SagaCourse(aggregateId, courseExecutionDto);
    }

    @Override
    public CourseDto createCourseDto(Course course) {
        return new CourseDto(course);
    }
}
