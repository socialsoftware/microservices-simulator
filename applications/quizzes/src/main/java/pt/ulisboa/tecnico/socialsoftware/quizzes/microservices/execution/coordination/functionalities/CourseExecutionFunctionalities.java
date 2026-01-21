package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.causal.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

@Service
public class CourseExecutionFunctionalities {
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;
    @Autowired
    private CommandGateway commandGateway;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseExecutionDto createCourseExecution(CourseExecutionDto courseExecutionDto) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(courseExecutionDto);
                CreateCourseExecutionFunctionalitySagas createCourseExecutionFunctionalitySagas = new CreateCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, courseExecutionDto, sagaUnitOfWork,
                        commandGateway);
                createCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);

                return createCourseExecutionFunctionalitySagas.getCreatedCourseExecution();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(courseExecutionDto);
                CreateCourseExecutionFunctionalityTCC createCourseExecutionFunctionalityTCC = new CreateCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, courseExecutionDto, causalUnitOfWork,
                        commandGateway);
                createCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);

                return createCourseExecutionFunctionalityTCC.getCreatedCourseExecution();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionByIdFunctionalitySagas getCourseExecutionByIdFunctionalitySagas = new GetCourseExecutionByIdFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork,
                        commandGateway);
                getCourseExecutionByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCourseExecutionByIdFunctionalitySagas.getCourseExecutionDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionByIdFunctionalityTCC getCourseExecutionByIdFunctionalityTCC = new GetCourseExecutionByIdFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, causalUnitOfWork,
                        commandGateway);
                getCourseExecutionByIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionByIdFunctionalityTCC.getCourseExecutionDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsFunctionalitySagas functionality = new GetCourseExecutionsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                functionality.executeWorkflow(sagaUnitOfWork);
                return functionality.getCourseExecutions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsFunctionalityTCC getCourseExecutionsFunctionalityTCC = new GetCourseExecutionsFunctionalityTCC(
                        causalUnitOfWorkService, causalUnitOfWork, commandGateway);
                getCourseExecutionsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionsFunctionalityTCC.getCourseExecutions();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFunctionalitySagas removeCourseExecutionFunctionalitySagas = new RemoveCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork,
                        commandGateway);
                removeCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFunctionalityTCC removeCourseExecutionFunctionalityTCC = new RemoveCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, causalUnitOfWork,
                        commandGateway);
                removeCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddStudentFunctionalitySagas addStudentFunctionalitySagas = new AddStudentFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId,
                        userAggregateId, sagaUnitOfWork, commandGateway);
                addStudentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AddStudentFunctionalityTCC addStudentFunctionalityTCC = new AddStudentFunctionalityTCC(
                        causalUnitOfWorkService,
                        executionAggregateId, userAggregateId, causalUnitOfWork, commandGateway);
                addStudentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsByUserFunctionalitySagas getCourseExecutionsByUserFunctionalitySagas = new GetCourseExecutionsByUserFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);
                getCourseExecutionsByUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCourseExecutionsByUserFunctionalitySagas.getCourseExecutions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsByUserFunctionalityTCC getCourseExecutionsByUserFunctionalityTCC = new GetCourseExecutionsByUserFunctionalityTCC(
                        causalUnitOfWorkService, userAggregateId, causalUnitOfWork,
                        commandGateway);
                getCourseExecutionsByUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionsByUserFunctionalityTCC.getCourseExecutions();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveStudentFromCourseExecutionFunctionalitySagas removeStudentFromCourseExecutionFunctionalitySagas = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService,
                        courseExecutionAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);
                removeStudentFromCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveStudentFromCourseExecutionFunctionalityTCC removeStudentFromCourseExecutionFunctionalityTCC = new RemoveStudentFromCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService,
                        courseExecutionAggregateId, userAggregateId, causalUnitOfWork, commandGateway);
                removeStudentFromCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AnonymizeStudentFunctionalitySagas anonymizeStudentFunctionalitySagas = new AnonymizeStudentFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId,
                        userAggregateId, sagaUnitOfWork, commandGateway);
                anonymizeStudentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AnonymizeStudentFunctionalityTCC anonymizeStudentFunctionalityTCC = new AnonymizeStudentFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId,
                        userAggregateId, causalUnitOfWork, commandGateway);
                anonymizeStudentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateStudentNameFunctionalitySagas updateStudentNameFunctionalitySagas = new UpdateStudentNameFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId,
                        userAggregateId, userDto, sagaUnitOfWork, commandGateway);
                updateStudentNameFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateStudentNameFunctionalityTCC updateStudentNameFunctionalityTCC = new UpdateStudentNameFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId,
                        userAggregateId, userDto, causalUnitOfWork, commandGateway);
                updateStudentNameFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeUserFromCourseExecution(Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveUserFromCourseExecutionFunctionalitySagas functionalitySagas = new RemoveUserFromCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);
                functionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveUserFromCourseExecutionFunctionalityTCC functionalityTCC = new RemoveUserFromCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, userAggregateId, causalUnitOfWork,
                        commandGateway);
                functionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CourseExecutionDto courseExecutionDto) {
        if (courseExecutionDto.getAcronym() == null) {
            throw new QuizzesException(COURSE_EXECUTION_MISSING_ACRONYM);
        }
        if (courseExecutionDto.getAcademicTerm() == null) {
            throw new QuizzesException(COURSE_EXECUTION_MISSING_ACADEMIC_TERM);
        }
        if (courseExecutionDto.getEndDate() == null) {
            throw new QuizzesException(COURSE_EXECUTION_MISSING_END_DATE);
        }

    }

}
