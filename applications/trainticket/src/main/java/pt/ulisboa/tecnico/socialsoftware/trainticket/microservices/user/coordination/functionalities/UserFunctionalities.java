package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.CreateUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.DeleteUserFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.GetUserByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.GetUsersFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.UpdateUserFunctionalitySagas;

import java.util.List;

@Service
public class UserFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public UserDto getUserById(Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getUserById");
        GetUserByIdFunctionalitySagas saga = new GetUserByIdFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getUserDto();
    }

    public List<UserDto> getUsers() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getUsers");
        GetUsersFunctionalitySagas saga = new GetUsersFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getUsers();
    }

    public UserDto createUser(UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateUserFunctionalitySagas saga = new CreateUserFunctionalitySagas(
                unitOfWorkService, userDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedUserDto();
    }

    public void updateUser(Integer userAggregateId, UserDto userDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateUserFunctionalitySagas saga = new UpdateUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, userDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteUser(Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteUserFunctionalitySagas saga = new DeleteUserFunctionalitySagas(
                unitOfWorkService, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
