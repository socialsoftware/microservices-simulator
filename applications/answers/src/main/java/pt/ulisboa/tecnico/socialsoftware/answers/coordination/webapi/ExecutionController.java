package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.ExecutionFunctionalities;
import java.util.Set;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.*;

import java.util.List;

@RestController
public class ExecutionController {
    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    @PostMapping("/executions/create")
    public ExecutionDto createExecution(@RequestBody ExecutionDto executionDto) throws Exception {
        ExecutionDto result = executionFunctionalities.createExecution(executionDto);
        return result;
    }

    @GetMapping("/executions/{executionAggregateId}")
    public ExecutionDto getExecutionByAggregateId(@PathVariable Integer executionAggregateId) {
        ExecutionDto result = executionFunctionalities.getExecutionByAggregateId(executionAggregateId);
        return result;
    }

    @GetMapping("/executions")
    public List<ExecutionDto> getExecutions() {
        List<ExecutionDto> result = executionFunctionalities.getExecutions();
        return result;
    }

    @PostMapping("/executions/{executionAggregateId}/delete")
    public void removeExecution(@PathVariable Integer executionAggregateId) throws Exception {
        executionFunctionalities.removeExecution(executionAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/students/add")
    public void addStudent(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        executionFunctionalities.addStudent(executionAggregateId, userAggregateId);
    }

    @GetMapping("/users/{userAggregateId}/executions")
    public Set<ExecutionDto> getExecutionsByUser(@PathVariable Integer userAggregateId) {
        Set<ExecutionDto> result = executionFunctionalities.getExecutionsByUser(userAggregateId);
        return result;
    }

    @PostMapping("/executions/{executionAggregateId}/students/remove")
    public void removeStudentFromExecution(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        executionFunctionalities.removeStudentFromExecution(executionAggregateId, userAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/anonymize")
    public void anonymizeStudent(@PathVariable Integer executionAggregateId, @RequestParam Integer userAggregateId) throws Exception {
        executionFunctionalities.anonymizeStudent(executionAggregateId, userAggregateId);
    }

    @PostMapping("/executions/{executionAggregateId}/students/{userAggregateId}/update/name")
    public void updateStudentName(@PathVariable Integer executionAggregateId, @PathVariable Integer userAggregateId, @RequestBody UserDto userDto) throws Exception {
        executionFunctionalities.updateStudentName(executionAggregateId, userAggregateId, userDto);
    }
}
