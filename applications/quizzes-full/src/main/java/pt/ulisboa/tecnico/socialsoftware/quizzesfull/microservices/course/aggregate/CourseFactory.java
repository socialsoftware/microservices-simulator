package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

public interface CourseFactory {
    Course createCourse(Integer aggregateId, CourseDto courseDto);
    Course createCourseFromExisting(Course existing);
    CourseDto createCourseDto(Course course);
}
