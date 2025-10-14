package pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.aggregate;

public interface CourseFactory {
    Course createCourse(Integer aggregateId,  Dto);
    Course createCourseFromExisting(Course existingCourse);
     createCourseDto(Course );
}
