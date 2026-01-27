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
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateUserRequestDto;
import java.util.List;

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

    public UserDto createUser(CreateUserRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateUserFunctionalitySagas createUserFunctionalitySagas = new CreateUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, userService, createRequest);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, userService, userAggregateId);
                getUserByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getUserByIdFunctionalitySagas.getUserDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto updateUser(UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);
                UpdateUserFunctionalitySagas updateUserFunctionalitySagas = new UpdateUserFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, userService, userDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, userService, userAggregateId);
                deleteUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getAllUsers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllUsersFunctionalitySagas getAllUsersFunctionalitySagas = new GetAllUsersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, userService);
                getAllUsersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllUsersFunctionalitySagas.getUsers();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getName() == null) {
            throw new AnswersException(USER_MISSING_NAME);
        }
        if (userDto.getUsername() == null) {
            throw new AnswersException(USER_MISSING_USERNAME);
        }
}

    private void checkInput(CreateUserRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new AnswersException(USER_MISSING_NAME);
        }
        if (createRequest.getUsername() == null) {
            throw new AnswersException(USER_MISSING_USERNAME);
        }
}
}