package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.Task;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.TaskFactory;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.SagaTask;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.sagas.dtos.SagaTaskDto;

@Service
@Profile("sagas")
public class SagasTaskFactory implements TaskFactory {
    @Override
    public Task createTask(Integer aggregateId, TaskDto taskDto) {
        return new SagaTask(aggregateId, taskDto);
    }

    @Override
    public Task createTaskFromExisting(Task existingTask) {
        return new SagaTask((SagaTask) existingTask);
    }

    @Override
    public TaskDto createTaskDto(Task task) {
        return new SagaTaskDto(task);
    }
}