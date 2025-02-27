package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.user.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.user.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.UNDEFINED_TRANSACTIONAL_MODEL;

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

    private MicroservicesSimulator.TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto createUser(UserDto userDto) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);

                CreateUserFunctionalitySagas createUserFunctionalitySagas = new CreateUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userDto, sagaUnitOfWork);

                createUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createUserFunctionalitySagas.getCreatedUserDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);

                CreateUserFunctionalityTCC createUserFunctionalityTCC = new CreateUserFunctionalityTCC(
                        userService, causalUnitOfWorkService, userDto, causalUnitOfWork);

                createUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createUserFunctionalityTCC.getCreatedUserDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto findByUserId(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindUserByIdFunctionalitySagas findUserByIdFunctionalitySagas = new FindUserByIdFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork);

                findUserByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findUserByIdFunctionalitySagas.getUserDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindUserByIdFunctionalityTCC findUserByIdFunctionalityTCC = new FindUserByIdFunctionalityTCC(
                        userService, causalUnitOfWorkService, userAggregateId, causalUnitOfWork);

                findUserByIdFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findUserByIdFunctionalityTCC.getUserDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void activateUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ActivateUserFunctionalitySagas activateUserFunctionalitySagas = new ActivateUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork);

                activateUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                ActivateUserFunctionalityTCC activateUserFunctionalityTCC = new ActivateUserFunctionalityTCC(
                        userService, causalUnitOfWorkService, userAggregateId, causalUnitOfWork);

                activateUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteUserFunctionalitySagas deleteUserFunctionalitySagas = new DeleteUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork);

                deleteUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteUserFunctionalityTCC deleteUserFunctionalityTCC = new DeleteUserFunctionalityTCC(
                        userService, causalUnitOfWorkService, userAggregateId, causalUnitOfWork);

                deleteUserFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getStudents() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetStudentsFunctionalitySagas getStudentsFunctionalitySagas = new GetStudentsFunctionalitySagas(
                        userService, sagaUnitOfWorkService, sagaUnitOfWork);

                getStudentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getStudentsFunctionalitySagas.getStudents();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetStudentsFunctionalityTCC getStudentsFunctionalityTCC = new GetStudentsFunctionalityTCC(
                        userService, causalUnitOfWorkService, causalUnitOfWork);

                getStudentsFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getStudentsFunctionalityTCC.getStudents();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getTeachers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTeachersFunctionalitySagas getTeachersFunctionalitySagas = new GetTeachersFunctionalitySagas(
                        userService, sagaUnitOfWorkService, sagaUnitOfWork);

                getTeachersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTeachersFunctionalitySagas.getTeachers();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTeachersFunctionalityTCC getTeachersFunctionalityTCC = new GetTeachersFunctionalityTCC(
                        userService, causalUnitOfWorkService, causalUnitOfWork);

                getTeachersFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getTeachersFunctionalityTCC.getTeachers();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getName() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_NAME);
        }
        if (userDto.getUsername() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_USERNAME);
        }
        if (userDto.getRole() == null) {
            throw new TutorException(ErrorMessage.USER_MISSING_ROLE);
        }
    }
}
