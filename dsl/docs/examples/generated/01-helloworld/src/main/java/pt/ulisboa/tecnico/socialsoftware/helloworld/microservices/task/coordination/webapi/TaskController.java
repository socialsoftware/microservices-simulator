package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi;

import org.springframework.web.bind.annotation.*;
import org.springframework.beans.factory.annotation.Autowired;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.functionalities.TaskFunctionalities;
import org.springframework.http.HttpStatus;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi.requestDtos.CreateTaskRequestDto;

@RestController
public class TaskController {
    @Autowired
    private TaskFunctionalities taskFunctionalities;

    @PostMapping("/tasks/create")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskDto createTask(@RequestBody CreateTaskRequestDto createRequest) {
        return taskFunctionalities.createTask(createRequest);
    }

    @GetMapping("/tasks/{taskAggregateId}")
    public TaskDto getTaskById(@PathVariable Integer taskAggregateId) {
        return taskFunctionalities.getTaskById(taskAggregateId);
    }

    @PutMapping("/tasks")
    public TaskDto updateTask(@RequestBody TaskDto taskDto) {
        return taskFunctionalities.updateTask(taskDto);
    }

    @DeleteMapping("/tasks/{taskAggregateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Integer taskAggregateId) {
        taskFunctionalities.deleteTask(taskAggregateId);
    }

    @GetMapping("/tasks")
    public List<TaskDto> getAllTasks() {
        return taskFunctionalities.getAllTasks();
    }
}
