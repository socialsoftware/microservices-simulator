package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.SagaCourse;

@Service
@Profile("sagas")
public class SagasCourseFactory implements CourseFactory {

    @Override
    public Course createCourse(Integer aggregateId, CourseDto courseDto) {
        return new SagaCourse(aggregateId, courseDto);
    }

    @Override
    public Course createCourseFromExisting(Course existing) {
        return new SagaCourse((SagaCourse) existing);
    }

    @Override
    public CourseDto createCourseDto(Course course) {
        return new CourseDto(course);
    }
}
