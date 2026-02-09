package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.execution.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.service.ExecutionService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionUserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateExecutionRequestDto;
import java.util.List;

@Service
public class ExecutionFunctionalities {
    @Autowired
    private ExecutionService executionService;

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
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionDto createExecution(CreateExecutionRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateExecutionFunctionalitySagas createExecutionFunctionalitySagas = new CreateExecutionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, createRequest);
                createExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createExecutionFunctionalitySagas.getCreatedExecutionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionDto getExecutionById(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetExecutionByIdFunctionalitySagas getExecutionByIdFunctionalitySagas = new GetExecutionByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, executionAggregateId);
                getExecutionByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getExecutionByIdFunctionalitySagas.getExecutionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionDto updateExecution(ExecutionDto executionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(executionDto);
                UpdateExecutionFunctionalitySagas updateExecutionFunctionalitySagas = new UpdateExecutionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, executionDto);
                updateExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateExecutionFunctionalitySagas.getUpdatedExecutionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteExecutionFunctionalitySagas deleteExecutionFunctionalitySagas = new DeleteExecutionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, executionAggregateId);
                deleteExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ExecutionDto> getAllExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllExecutionsFunctionalitySagas getAllExecutionsFunctionalitySagas = new GetAllExecutionsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService);
                getAllExecutionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllExecutionsFunctionalitySagas.getExecutions();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionUserDto addExecutionUser(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddExecutionUserFunctionalitySagas addExecutionUserFunctionalitySagas = new AddExecutionUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService,
                        executionId, userAggregateId, userDto);
                addExecutionUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addExecutionUserFunctionalitySagas.getAddedUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ExecutionUserDto> addExecutionUsers(Integer executionId, List<ExecutionUserDto> userDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddExecutionUsersFunctionalitySagas addExecutionUsersFunctionalitySagas = new AddExecutionUsersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService,
                        executionId, userDtos);
                addExecutionUsersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addExecutionUsersFunctionalitySagas.getAddedUserDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionUserDto getExecutionUser(Integer executionId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetExecutionUserFunctionalitySagas getExecutionUserFunctionalitySagas = new GetExecutionUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService,
                        executionId, userAggregateId);
                getExecutionUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getExecutionUserFunctionalitySagas.getUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionUserDto updateExecutionUser(Integer executionId, Integer userAggregateId, ExecutionUserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateExecutionUserFunctionalitySagas updateExecutionUserFunctionalitySagas = new UpdateExecutionUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService,
                        executionId, userAggregateId, userDto);
                updateExecutionUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateExecutionUserFunctionalitySagas.getUpdatedUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeExecutionUser(Integer executionId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveExecutionUserFunctionalitySagas removeExecutionUserFunctionalitySagas = new RemoveExecutionUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService,
                        executionId, userAggregateId);
                removeExecutionUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(ExecutionDto executionDto) {
        if (executionDto.getAcronym() == null) {
            throw new AnswersException(EXECUTION_MISSING_ACRONYM);
        }
        if (executionDto.getAcademicTerm() == null) {
            throw new AnswersException(EXECUTION_MISSING_ACADEMICTERM);
        }
}

    private void checkInput(CreateExecutionRequestDto createRequest) {
        if (createRequest.getAcronym() == null) {
            throw new AnswersException(EXECUTION_MISSING_ACRONYM);
        }
        if (createRequest.getAcademicTerm() == null) {
            throw new AnswersException(EXECUTION_MISSING_ACADEMICTERM);
        }
}
}