package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.sagas.SagaCourse;

@Service
@Profile("sagas")
public class SagasCourseFactory {

    public SagaCourse createCourse(Integer aggregateId, String name, String type) {
        return new SagaCourse(aggregateId, name, type);
    }

    public SagaCourse createCourseCopy(SagaCourse existing) {
        return new SagaCourse(existing);
    }

    public CourseDto createCourseDto(Course course) {
        return new CourseDto(course);
    }
}
