package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.webapi;

import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities.CourseExecutionFunctionalities;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

@RestController
public class CourseExecutionController {
    @Autowired
    private CourseExecutionFunctionalities courseExecutionFunctionalities;

    @PostMapping(value = "/executions/create")
    public CourseExecutionDto createCourseExecution(@RequestBody CourseExecutionDto executionDto) throws Exception {
        CourseExecutionDto courseExecutionDto = courseExecutionFunctionalities.createCourseExecution(executionDto);
        return courseExecutionDto;
    }

    @GetMapping(value = "/executions/{executionAggregateId}")
    public CourseExecutionDto getCourseExecutionByAggregateId(@PathVariable Integer executionAggregateId) {
        return courseExecutionFunctionalities.getCourseExecutionByAggregateId(executionAggregateId);
    }

    @GetMapping(value = "/executions")
    public List<CourseExecutionDto> getCourseExecutions() {
        return courseExecutionFunctionalities.getCourseExecutions();
    }

    @PostMapping("/executions/{executionAggregateId}/delete")
    public void removeCourseExecution(@PathVariable Integer executionAggregateId) throws Exception {
        courseExecutionFunctionalities.removeCourseExecution(executionAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/students/add")
    public void enrollStudent(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        courseExecutionFunctionalities.addStudent(executionAggregateId, userAggregateId);
    }

    @GetMapping("/users/{userAggregateId}/executions")
    public Set<CourseExecutionDto> getUserCourseExecutions(@PathVariable Integer userAggregateId) {
        return courseExecutionFunctionalities.getCourseExecutionsByUser(userAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/students/remove")
    public void removeStudentFromCourseExecution(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        courseExecutionFunctionalities.removeStudentFromCourseExecution(executionAggregateId, userAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/anonymize")
    public void anonymizeExecutionStudent(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        courseExecutionFunctionalities.anonymizeStudent(executionAggregateId, userAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/students/{userAggregateId}/update/name")
    public void updateExecutionStudentName(@PathVariable Integer executionAggregateId, @PathVariable Integer userAggregateId, @RequestBody UserDto userDto) throws Exception {
        courseExecutionFunctionalities.updateStudentName(executionAggregateId, userAggregateId, userDto);
    }
}
