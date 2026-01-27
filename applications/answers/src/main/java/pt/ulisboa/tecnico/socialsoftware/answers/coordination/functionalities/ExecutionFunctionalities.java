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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.course.service.CourseService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.ExecutionDto;
import java.util.List;

@Service
public class ExecutionFunctionalities {
    @Autowired
    private ExecutionService executionService;

    @Autowired
    private CourseService courseService;

    @Autowired
    private UserService userService;

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
                checkInput(executionDto);
                CreateExecutionFunctionalitySagas createExecutionFunctionalitySagas = new CreateExecutionFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, courseService, userService, executionDto);
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

    public List<ExecutionDto> searchExecutions(String acronym, String academicTerm, Integer courseAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchExecutionsFunctionalitySagas searchExecutionsFunctionalitySagas = new SearchExecutionsFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, executionService, acronym, academicTerm, courseAggregateId);
                searchExecutionsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchExecutionsFunctionalitySagas.getSearchedExecutionDtos();
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
}