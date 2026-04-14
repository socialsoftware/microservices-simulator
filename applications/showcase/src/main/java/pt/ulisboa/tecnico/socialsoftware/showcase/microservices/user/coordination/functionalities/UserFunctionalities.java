package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.coordination.webapi.requestDtos.CreateUserRequestDto;
import java.util.List;

@Service
public class UserFunctionalities {
    @Autowired
    private UserService userService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto createUser(CreateUserRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateUserFunctionalitySagas createUserFunctionalitySagas = new CreateUserFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createUserFunctionalitySagas.getCreatedUserDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto getUserById(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetUserByIdFunctionalitySagas getUserByIdFunctionalitySagas = new GetUserByIdFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);
                getUserByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getUserByIdFunctionalitySagas.getUserDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto updateUser(UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userDto);
                UpdateUserFunctionalitySagas updateUserFunctionalitySagas = new UpdateUserFunctionalitySagas(
                        sagaUnitOfWorkService, userDto, sagaUnitOfWork, commandGateway);
                updateUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateUserFunctionalitySagas.getUpdatedUserDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteUserFunctionalitySagas deleteUserFunctionalitySagas = new DeleteUserFunctionalitySagas(
                        sagaUnitOfWorkService, userAggregateId, sagaUnitOfWork, commandGateway);
                deleteUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<UserDto> getAllUsers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllUsersFunctionalitySagas getAllUsersFunctionalitySagas = new GetAllUsersFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllUsersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllUsersFunctionalitySagas.getUsers();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(UserDto userDto) {
        if (userDto.getUsername() == null) {
            throw new ShowcaseException(USER_MISSING_USERNAME);
        }
        if (userDto.getEmail() == null) {
            throw new ShowcaseException(USER_MISSING_EMAIL);
        }
}

    private void checkInput(CreateUserRequestDto createRequest) {
        if (createRequest.getUsername() == null) {
            throw new ShowcaseException(USER_MISSING_USERNAME);
        }
        if (createRequest.getEmail() == null) {
            throw new ShowcaseException(USER_MISSING_EMAIL);
        }
}
}