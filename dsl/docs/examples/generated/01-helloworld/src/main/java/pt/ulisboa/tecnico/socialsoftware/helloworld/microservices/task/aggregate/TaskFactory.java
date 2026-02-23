package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate;

import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;

public interface TaskFactory {
    Task createTask(Integer aggregateId, TaskDto taskDto);
    Task createTaskFromExisting(Task existingTask);
    TaskDto createTaskDto(Task task);
}
