package pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.events.TaskDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.helloworld.events.TaskUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.exception.HelloworldException;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.coordination.webapi.requestDtos.CreateTaskRequestDto;


@Service
@Transactional(noRollbackFor = HelloworldException.class)
public class TaskService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private TaskFactory taskFactory;

    public TaskService() {}

    public TaskDto createTask(CreateTaskRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            TaskDto taskDto = new TaskDto();
            taskDto.setTitle(createRequest.getTitle());
            taskDto.setDescription(createRequest.getDescription());
            taskDto.setDone(createRequest.getDone());

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Task task = taskFactory.createTask(aggregateId, taskDto);
            unitOfWorkService.registerChanged(task, unitOfWork);
            return taskFactory.createTaskDto(task);
        } catch (HelloworldException e) {
            throw e;
        } catch (Exception e) {
            throw new HelloworldException("Error creating task: " + e.getMessage());
        }
    }

    public TaskDto getTaskById(Integer id, UnitOfWork unitOfWork) {
        try {
            Task task = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return taskFactory.createTaskDto(task);
        } catch (HelloworldException e) {
            throw e;
        } catch (Exception e) {
            throw new HelloworldException("Error retrieving task: " + e.getMessage());
        }
    }

    public List<TaskDto> getAllTasks(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = taskRepository.findAll().stream()
                .map(Task::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(taskFactory::createTaskDto)
                .collect(Collectors.toList());
        } catch (HelloworldException e) {
            throw e;
        } catch (Exception e) {
            throw new HelloworldException("Error retrieving task: " + e.getMessage());
        }
    }

    public TaskDto updateTask(TaskDto taskDto, UnitOfWork unitOfWork) {
        try {
            Integer id = taskDto.getAggregateId();
            Task oldTask = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Task newTask = taskFactory.createTaskFromExisting(oldTask);
            if (taskDto.getTitle() != null) {
                newTask.setTitle(taskDto.getTitle());
            }
            if (taskDto.getDescription() != null) {
                newTask.setDescription(taskDto.getDescription());
            }
            newTask.setDone(taskDto.getDone());

            unitOfWorkService.registerChanged(newTask, unitOfWork);            TaskUpdatedEvent event = new TaskUpdatedEvent(newTask.getAggregateId(), newTask.getTitle(), newTask.getDescription(), newTask.getDone());
            event.setPublisherAggregateVersion(newTask.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return taskFactory.createTaskDto(newTask);
        } catch (HelloworldException e) {
            throw e;
        } catch (Exception e) {
            throw new HelloworldException("Error updating task: " + e.getMessage());
        }
    }

    public void deleteTask(Integer id, UnitOfWork unitOfWork) {
        try {
            Task oldTask = (Task) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Task newTask = taskFactory.createTaskFromExisting(oldTask);
            newTask.remove();
            unitOfWorkService.registerChanged(newTask, unitOfWork);            unitOfWorkService.registerEvent(new TaskDeletedEvent(newTask.getAggregateId()), unitOfWork);
        } catch (HelloworldException e) {
            throw e;
        } catch (Exception e) {
            throw new HelloworldException("Error deleting task: " + e.getMessage());
        }
    }








}