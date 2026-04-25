package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.course.aggregate;

public interface CourseFactory {
    Course createCourse(Integer aggregateId, String name, String type);
    Course createCourseCopy(Course existing);
    CourseDto createCourseDto(Course course);
}
