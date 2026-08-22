package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.GetUserByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.coordination.sagas.GetUsersFunctionalitySagas;

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
}
