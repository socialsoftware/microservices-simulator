package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.causal.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.Arrays;
import java.util.List;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

@Service
public class UserFunctionalities {
    @Autowired
    private UserService userService;
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

    public UserDto createUser(UserDto userDto) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);

                CreateUserFunctionalitySagas createUserFunctionalitySagas = new CreateUserFunctionalitySagas(
                        sagaUnitOfWorkService, userDto, sagaUnitOfWork, commandGateway);

                createUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createUserFunctionalitySagas.getCreatedUserDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);

                CreateUserFunctionalityTCC createUserFunctionalityTCC = new CreateUserFunctionalityTCC(
                        causalUnitOfWorkService, userDto, causalUnitOfWork, commandGateway);

                createUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createUserFunctionalityTCC.getCreatedUserDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto findByUserId(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindUserByIdFunctionalitySagas findUserByIdFunctionalitySagas = new FindUserByIdFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);

                findUserByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findUserByIdFunctionalitySagas.getUserDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindUserByIdFunctionalityTCC findUserByIdFunctionalityTCC = new FindUserByIdFunctionalityTCC(
                        causalUnitOfWorkService, userAggregateId, causalUnitOfWork, commandGateway);

                findUserByIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findUserByIdFunctionalityTCC.getUserDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void activateUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ActivateUserFunctionalitySagas activateUserFunctionalitySagas = new ActivateUserFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);

                activateUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                ActivateUserFunctionalityTCC activateUserFunctionalityTCC = new ActivateUserFunctionalityTCC(
                        causalUnitOfWorkService, userAggregateId, causalUnitOfWork, commandGateway);

                activateUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deactivateUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeactivateUserFunctionalitySagas deactivateUserFunctionalitySagas = new DeactivateUserFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);

                deactivateUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                DeactivateUserFunctionalityTCC deactivateUserFunctionalityTCC = new DeactivateUserFunctionalityTCC(
                        causalUnitOfWorkService, userAggregateId, causalUnitOfWork, commandGateway);

                deactivateUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteUserFunctionalitySagas deleteUserFunctionalitySagas = new DeleteUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);

                deleteUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteUserFunctionalityTCC deleteUserFunctionalityTCC = new DeleteUserFunctionalityTCC(
                        causalUnitOfWorkService, userAggregateId, causalUnitOfWork, commandGateway);

                deleteUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetStudentsFunctionalitySagas getStudentsFunctionalitySagas = new GetStudentsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);

                getStudentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getStudentsFunctionalitySagas.getStudents();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetStudentsFunctionalityTCC getStudentsFunctionalityTCC = new GetStudentsFunctionalityTCC(
                        causalUnitOfWorkService, causalUnitOfWork, commandGateway);

                getStudentsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getStudentsFunctionalityTCC.getStudents();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTeachersFunctionalitySagas getTeachersFunctionalitySagas = new GetTeachersFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);

                getTeachersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTeachersFunctionalitySagas.getTeachers();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTeachersFunctionalityTCC getTeachersFunctionalityTCC = new GetTeachersFunctionalityTCC(
                        causalUnitOfWorkService, causalUnitOfWork, commandGateway);

                getTeachersFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getTeachersFunctionalityTCC.getTeachers();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getName() == null) {
            throw new QuizzesException(QuizzesErrorMessage.USER_MISSING_NAME);
        }
        if (userDto.getUsername() == null) {
            throw new QuizzesException(QuizzesErrorMessage.USER_MISSING_USERNAME);
        }
        if (userDto.getRole() == null) {
            throw new QuizzesException(QuizzesErrorMessage.USER_MISSING_ROLE);
        }
    }
}
