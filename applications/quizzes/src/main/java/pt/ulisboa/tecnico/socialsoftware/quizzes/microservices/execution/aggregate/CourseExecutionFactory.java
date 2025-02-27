package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate;

public interface CourseExecutionFactory {
    CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse);
    CourseExecution createCourseExecutionFromExisting(CourseExecution existingCourseExecution);
    CourseExecutionDto createCourseExecutionDto(CourseExecution courseExecution);
}
