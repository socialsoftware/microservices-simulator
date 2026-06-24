package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

public interface CourseExecutionFactory {
    CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto dto, CourseExecutionCourse courseExecutionCourse);
    CourseExecution createCourseExecutionFromExisting(CourseExecution existing);
    CourseExecutionDto createCourseExecutionDto(CourseExecution courseExecution);
}
