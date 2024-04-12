package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service;

import java.util.List;
import java.util.Set;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

public interface CourseExecutionFunctionalitiesInterface {
    CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto) throws Exception;
    CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId);
    List<CourseExecutionDto> getCourseExecutions();
    void removeCourseExecution(Integer executionAggregateId) throws Exception;
    void addStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception;
    Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId);
    void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) throws Exception;
    void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) throws Exception;
    void updateStudentName(Integer executionAggregateId, Integer userAggregateId , UserDto userDto) throws Exception;
}
