package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.Course;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.CourseDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate.CourseFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaCourse;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaCourseDto;

@Service
@Profile("sagas")
public class SagasCourseFactory extends CourseFactory {
@Override
public Course createCourse(Integer aggregateId, CourseDto courseDto) {
return new SagaCourse(courseDto);
}

@Override
public Course createCourseFromExisting(Course existingCourse) {
return new SagaCourse((SagaCourse) existingCourse);
}

@Override
public CourseDto createCourseDto(Course course) {
return new SagaCourseDto(course);
}
}