package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.CourseType;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate.sagas.SagaCourse;

@Service
@Profile("sagas")
public class SagasCourseFactory implements CourseFactory {
    @Override
    public SagaCourse createCourse(Integer aggregateId, String name, CourseType type) {
        return new SagaCourse(aggregateId, name, type);
    }

    @Override
    public SagaCourse createCourseCopy(Course existing) {
        return new SagaCourse((SagaCourse) existing);
    }

    @Override
    public CourseDto createCourseDto(Course course) {
        return new CourseDto(course);
    }
}
