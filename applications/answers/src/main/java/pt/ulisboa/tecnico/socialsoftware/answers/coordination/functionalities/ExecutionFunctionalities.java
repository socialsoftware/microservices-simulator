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

    public ExecutionDto createExecution(ExecutionDto executionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateExecutionFunctionalitySagas createExecutionFunctionalitySagas = new CreateExecutionFunctionalitySagas(
                        executionService, sagaUnitOfWorkService, executionDto, sagaUnitOfWork);
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
                        executionService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                getExecutionByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getExecutionByIdFunctionalitySagas.getExecutionDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public ExecutionDto updateExecution(Integer executionAggregateId, ExecutionDto executionDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateExecutionFunctionalitySagas updateExecutionFunctionalitySagas = new UpdateExecutionFunctionalitySagas(
                        executionService, sagaUnitOfWorkService, executionAggregateId, executionDto, sagaUnitOfWork);
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
                        executionService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                deleteExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<ExecutionDto> searchExecutions(String acronym, String academicTerm, Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchExecutionsFunctionalitySagas searchExecutionsFunctionalitySagas = new SearchExecutionsFunctionalitySagas(
                        executionService, sagaUnitOfWorkService, acronym, academicTerm, courseAggregateId, sagaUnitOfWork);
                searchExecutionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchExecutionsFunctionalitySagas.getSearchedExecutionDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}