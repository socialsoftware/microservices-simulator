package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.causal.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate.causal.CausalCourse;

@Service
@Profile("tcc")
public class CausalCourseFactory implements CourseFactory {

    @Override
    public Course createCourse(Integer aggregateId, CourseDto courseDto) {
        throw new UnsupportedOperationException("TCC not implemented");
    }

    @Override
    public Course createCourseFromExisting(Course existing) {
        throw new UnsupportedOperationException("TCC not implemented");
    }

    @Override
    public CourseDto createCourseDto(Course course) {
        throw new UnsupportedOperationException("TCC not implemented");
    }
}
