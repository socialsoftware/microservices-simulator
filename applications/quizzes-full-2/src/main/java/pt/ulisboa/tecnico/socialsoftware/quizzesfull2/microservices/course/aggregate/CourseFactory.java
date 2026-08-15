package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.course.aggregate;

public interface CourseFactory {
    Course createCourse(Integer aggregateId, String name, CourseType type);

    Course createCourseCopy(Course existing);

    CourseDto createCourseDto(Course course);
}
