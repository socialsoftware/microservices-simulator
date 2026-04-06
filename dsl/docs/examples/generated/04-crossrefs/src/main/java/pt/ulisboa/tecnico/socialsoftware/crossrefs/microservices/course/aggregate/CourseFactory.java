package pt.ulisboa.tecnico.socialsoftware.crossrefs.microservices.course.aggregate;

import pt.ulisboa.tecnico.socialsoftware.crossrefs.shared.dtos.CourseDto;

public interface CourseFactory {
    Course createCourse(Integer aggregateId, CourseDto courseDto);
    Course createCourseFromExisting(Course existingCourse);
    CourseDto createCourseDto(Course course);
}
