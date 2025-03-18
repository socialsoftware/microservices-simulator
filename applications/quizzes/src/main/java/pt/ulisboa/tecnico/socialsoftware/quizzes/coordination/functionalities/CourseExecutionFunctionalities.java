package pt.ulisboa.tecnico.socialsoftware.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.AddStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.AnonymizeStudentFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.CreateCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionByIdFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionsByUserFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.GetCourseExecutionsFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.RemoveCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.RemoveStudentFromCourseExecutionFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.causal.coordination.execution.UpdateStudentNameFunctionalityTCC;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AddStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.AnonymizeStudentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.CreateCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionsByUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.GetCourseExecutionsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.RemoveCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.RemoveStudentFromCourseExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.coordination.execution.UpdateStudentNameFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;

@Service
public class CourseExecutionFunctionalities {
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private UserService userService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private CourseExecutionFactory courseExecutionFactory;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

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
                        courseExecutionService, sagaUnitOfWorkService, courseExecutionDto, sagaUnitOfWork);
                createCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);

                return createCourseExecutionFunctionalitySagas.getCreatedCourseExecution();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(courseExecutionDto);
                CreateCourseExecutionFunctionalityTCC createCourseExecutionFunctionalityTCC = new CreateCourseExecutionFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, courseExecutionDto, causalUnitOfWork);
                createCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);

                return createCourseExecutionFunctionalityTCC.getCreatedCourseExecution();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CourseExecutionDto getCourseExecutionByAggregateId(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionByIdFunctionalitySagas getCourseExecutionByIdFunctionalitySagas = new GetCourseExecutionByIdFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                getCourseExecutionByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCourseExecutionByIdFunctionalitySagas.getCourseExecutionDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionByIdFunctionalityTCC getCourseExecutionByIdFunctionalityTCC = new GetCourseExecutionByIdFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, executionAggregateId, causalUnitOfWork);
                getCourseExecutionByIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionByIdFunctionalityTCC.getCourseExecutionDto();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CourseExecutionDto> getCourseExecutions() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsFunctionalitySagas functionality = new GetCourseExecutionsFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, sagaUnitOfWork);
                functionality.executeWorkflow(sagaUnitOfWork);
                return functionality.getCourseExecutions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsFunctionalityTCC getCourseExecutionsFunctionalityTCC = new GetCourseExecutionsFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, causalUnitOfWork);
                getCourseExecutionsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionsFunctionalityTCC.getCourseExecutions();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFunctionalitySagas removeCourseExecutionFunctionalitySagas = new RemoveCourseExecutionFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                removeCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveCourseExecutionFunctionalityTCC removeCourseExecutionFunctionalityTCC = new RemoveCourseExecutionFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, executionAggregateId, causalUnitOfWork);
                removeCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addStudent(Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddStudentFunctionalitySagas addStudentFunctionalitySagas = new AddStudentFunctionalitySagas(
                        courseExecutionService, userService, sagaUnitOfWorkService, executionAggregateId, userAggregateId, sagaUnitOfWork);
                addStudentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AddStudentFunctionalityTCC addStudentFunctionalityTCC = new AddStudentFunctionalityTCC(
                        courseExecutionService, userService, causalUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, causalUnitOfWork);
                addStudentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public Set<CourseExecutionDto> getCourseExecutionsByUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsByUserFunctionalitySagas getCourseExecutionsByUserFunctionalitySagas = new GetCourseExecutionsByUserFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork);
                getCourseExecutionsByUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCourseExecutionsByUserFunctionalitySagas.getCourseExecutions();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCourseExecutionsByUserFunctionalityTCC getCourseExecutionsByUserFunctionalityTCC = new GetCourseExecutionsByUserFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, userAggregateId, causalUnitOfWork);
                getCourseExecutionsByUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getCourseExecutionsByUserFunctionalityTCC.getCourseExecutions();
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeStudentFromCourseExecution(Integer courseExecutionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveStudentFromCourseExecutionFunctionalitySagas removeStudentFromCourseExecutionFunctionalitySagas = new RemoveStudentFromCourseExecutionFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, courseExecutionFactory, courseExecutionAggregateId, userAggregateId, sagaUnitOfWork);
                removeStudentFromCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveStudentFromCourseExecutionFunctionalityTCC removeStudentFromCourseExecutionFunctionalityTCC = new RemoveStudentFromCourseExecutionFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, courseExecutionFactory, courseExecutionAggregateId, userAggregateId, causalUnitOfWork);
                removeStudentFromCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void anonymizeStudent(Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AnonymizeStudentFunctionalitySagas anonymizeStudentFunctionalitySagas = new AnonymizeStudentFunctionalitySagas(
                        courseExecutionService, sagaUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, sagaUnitOfWork);
                anonymizeStudentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AnonymizeStudentFunctionalityTCC anonymizeStudentFunctionalityTCC = new AnonymizeStudentFunctionalityTCC(
                        courseExecutionService, causalUnitOfWorkService, courseExecutionFactory, executionAggregateId, userAggregateId, causalUnitOfWork);
                anonymizeStudentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateStudentName(Integer executionAggregateId, Integer userAggregateId, UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateStudentNameFunctionalitySagas updateStudentNameFunctionalitySagas = new UpdateStudentNameFunctionalitySagas(
                        courseExecutionService, courseExecutionFactory, sagaUnitOfWorkService, executionAggregateId, userAggregateId, userDto, sagaUnitOfWork);
                updateStudentNameFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateStudentNameFunctionalityTCC updateStudentNameFunctionalityTCC = new UpdateStudentNameFunctionalityTCC(
                        courseExecutionService, courseExecutionFactory, causalUnitOfWorkService, executionAggregateId, userAggregateId, userDto, causalUnitOfWork);
                updateStudentNameFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
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
