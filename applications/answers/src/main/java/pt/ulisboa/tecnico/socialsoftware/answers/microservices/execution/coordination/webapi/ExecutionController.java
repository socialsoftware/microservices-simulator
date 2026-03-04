package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.functionalities.ExecutionFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.coordination.webapi.requestDtos.CreateExecutionRequestDto;

@RestController
public class ExecutionController {
    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    @PostMapping("/executions/create")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionDto createExecution(@RequestBody CreateExecutionRequestDto createRequest) {
        return executionFunctionalities.createExecution(createRequest);
    }

    @GetMapping("/executions/{executionAggregateId}")
    public ExecutionDto getExecutionById(@PathVariable Integer executionAggregateId) {
        return executionFunctionalities.getExecutionById(executionAggregateId);
    }

    @PutMapping("/executions")
    public ExecutionDto updateExecution(@RequestBody ExecutionDto executionDto) {
        return executionFunctionalities.updateExecution(executionDto);
    }

    @DeleteMapping("/executions/{executionAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExecution(@PathVariable Integer executionAggregateId) {
        executionFunctionalities.deleteExecution(executionAggregateId);
    }

    @GetMapping("/executions")
    public List<ExecutionDto> getAllExecutions() {
        return executionFunctionalities.getAllExecutions();
    }

    @PostMapping("/executions/{executionId}/users")
    @ResponseStatus(HttpStatus.CREATED)
    public ExecutionUserDto addExecutionUser(@PathVariable Integer executionId, @RequestParam Integer userAggregateId, @RequestBody ExecutionUserDto userDto) {
        return executionFunctionalities.addExecutionUser(executionId, userAggregateId, userDto);
    }

    @PostMapping("/executions/{executionId}/users/batch")
    public List<ExecutionUserDto> addExecutionUsers(@PathVariable Integer executionId, @RequestBody List<ExecutionUserDto> userDtos) {
        return executionFunctionalities.addExecutionUsers(executionId, userDtos);
    }

    @GetMapping("/executions/{executionId}/users/{userAggregateId}")
    public ExecutionUserDto getExecutionUser(@PathVariable Integer executionId, @PathVariable Integer userAggregateId) {
        return executionFunctionalities.getExecutionUser(executionId, userAggregateId);
    }

    @PutMapping("/executions/{executionId}/users/{userAggregateId}")
    public ExecutionUserDto updateExecutionUser(@PathVariable Integer executionId, @PathVariable Integer userAggregateId, @RequestBody ExecutionUserDto userDto) {
        return executionFunctionalities.updateExecutionUser(executionId, userAggregateId, userDto);
    }

    @DeleteMapping("/executions/{executionId}/users/{userAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeExecutionUser(@PathVariable Integer executionId, @PathVariable Integer userAggregateId) {
        executionFunctionalities.removeExecutionUser(executionId, userAggregateId);
    }
}
