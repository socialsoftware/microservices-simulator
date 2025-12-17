package pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities.ExecutionFunctionalities;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;

@RestController
public class ExecutionController {
    @Autowired
    private ExecutionFunctionalities executionFunctionalities;

    @PostMapping("/executions/create")
    public ExecutionDto createExecution(@RequestParam Integer courseAggregateId, @RequestBody ExecutionDto executionDto) {
        return executionFunctionalities.createExecution(courseAggregateId, executionDto);
    }

    @GetMapping("/executions/{executionAggregateId}")
    public ExecutionDto getExecutionById(@PathVariable Integer executionAggregateId) {
        return executionFunctionalities.getExecutionById(executionAggregateId);
    }

    @PutMapping("/executions/{executionAggregateId}")
    public ExecutionDto updateExecution(@PathVariable Integer executionAggregateId, @RequestBody ExecutionDto executionDto) {
        return executionFunctionalities.updateExecution(executionAggregateId, executionDto);
    }

    @DeleteMapping("/executions/{executionAggregateId}")
    public void deleteExecution(@PathVariable Integer executionAggregateId) {
        executionFunctionalities.deleteExecution(executionAggregateId);
    }

    @GetMapping("/executions")
    public List<ExecutionDto> searchExecutions(@RequestParam(required = false) String acronym, @RequestParam(required = false) String academicTerm, @RequestParam(required = false) Integer courseAggregateId) {
        return executionFunctionalities.searchExecutions(acronym, academicTerm, courseAggregateId);
    }
}
