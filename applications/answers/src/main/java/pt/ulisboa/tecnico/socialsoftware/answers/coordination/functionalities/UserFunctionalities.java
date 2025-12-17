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
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.user.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import java.util.List;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.UserRole;

@Service
public class UserFunctionalities {
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

    public UserDto createUser(UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateUserFunctionalitySagas createUserFunctionalitySagas = new CreateUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userDto, sagaUnitOfWork);
                createUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createUserFunctionalitySagas.getCreatedUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto getUserById(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetUserByIdFunctionalitySagas getUserByIdFunctionalitySagas = new GetUserByIdFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork);
                getUserByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getUserByIdFunctionalitySagas.getUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto updateUser(Integer userAggregateId, UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateUserFunctionalitySagas updateUserFunctionalitySagas = new UpdateUserFunctionalitySagas(
                        userService, sagaUnitOfWorkService, userAggregateId, userDto, sagaUnitOfWork);
                updateUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateUserFunctionalitySagas.getUpdatedUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
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
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> searchUsers(String name, String username, UserRole role, Boolean active) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SearchUsersFunctionalitySagas searchUsersFunctionalitySagas = new SearchUsersFunctionalitySagas(
                        userService, sagaUnitOfWorkService, name, username, role, active, sagaUnitOfWork);
                searchUsersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return searchUsersFunctionalitySagas.getSearchedUserDtos();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}