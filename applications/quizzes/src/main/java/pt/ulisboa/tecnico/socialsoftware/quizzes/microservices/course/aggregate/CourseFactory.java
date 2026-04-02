package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.course.aggregate;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;

public interface CourseFactory {
    Course createCourse(Integer aggregateId, CourseExecutionDto courseExecutionDto);
    Course createCourseFromExisting(Course existing);
    CourseDto createCourseDto(Course course);
}
