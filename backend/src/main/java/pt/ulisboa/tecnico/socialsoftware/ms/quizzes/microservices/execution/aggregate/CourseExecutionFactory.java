package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate;

public interface CourseExecutionFactory {
    CourseExecution createCourseExecution(Integer aggregateId, CourseExecutionDto courseExecutionDto, CourseExecutionCourse courseExecutionCourse);
    CourseExecution createCourseExecutionFromExisting(CourseExecution existingCourseExecution);
}
