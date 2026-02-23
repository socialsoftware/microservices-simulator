package pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.exception.HelloworldErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.exception.HelloworldException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.sagas.coordination.task.*;
import pt.ulisboa.tecnico.socialsoftware.helloworld.microservices.task.service.TaskService;
import pt.ulisboa.tecnico.socialsoftware.helloworld.shared.dtos.TaskDto;
import pt.ulisboa.tecnico.socialsoftware.helloworld.coordination.webapi.requestDtos.CreateTaskRequestDto;
import java.util.List;

@Service
public class TaskFunctionalities {
    @Autowired
    private TaskService taskService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TaskDto createTask(CreateTaskRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateTaskFunctionalitySagas createTaskFunctionalitySagas = new CreateTaskFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, taskService, createRequest);
                createTaskFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTaskFunctionalitySagas.getCreatedTaskDto();
            default: throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TaskDto getTaskById(Integer taskAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTaskByIdFunctionalitySagas getTaskByIdFunctionalitySagas = new GetTaskByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, taskService, taskAggregateId);
                getTaskByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTaskByIdFunctionalitySagas.getTaskDto();
            default: throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TaskDto updateTask(TaskDto taskDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(taskDto);
                UpdateTaskFunctionalitySagas updateTaskFunctionalitySagas = new UpdateTaskFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, taskService, taskDto);
                updateTaskFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTaskFunctionalitySagas.getUpdatedTaskDto();
            default: throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTask(Integer taskAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTaskFunctionalitySagas deleteTaskFunctionalitySagas = new DeleteTaskFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, taskService, taskAggregateId);
                deleteTaskFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TaskDto> getAllTasks() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllTasksFunctionalitySagas getAllTasksFunctionalitySagas = new GetAllTasksFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, taskService);
                getAllTasksFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllTasksFunctionalitySagas.getTasks();
            default: throw new HelloworldException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TaskDto taskDto) {
        if (taskDto.getTitle() == null) {
            throw new HelloworldException(TASK_MISSING_TITLE);
        }
        if (taskDto.getDescription() == null) {
            throw new HelloworldException(TASK_MISSING_DESCRIPTION);
        }
}

    private void checkInput(CreateTaskRequestDto createRequest) {
        if (createRequest.getTitle() == null) {
            throw new HelloworldException(TASK_MISSING_TITLE);
        }
        if (createRequest.getDescription() == null) {
            throw new HelloworldException(TASK_MISSING_DESCRIPTION);
        }
}
}