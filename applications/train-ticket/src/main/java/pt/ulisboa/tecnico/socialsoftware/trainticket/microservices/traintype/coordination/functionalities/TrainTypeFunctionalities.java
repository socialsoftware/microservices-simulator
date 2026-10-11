package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.CreateTrainTypeFunctionalitySagas;

@Service
public class TrainTypeFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    public TrainTypeDto createTrainType(TrainTypeDto trainTypeDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createTrainType");
        CreateTrainTypeFunctionalitySagas saga = new CreateTrainTypeFunctionalitySagas(
                unitOfWorkService, trainTypeDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTrainTypeDto();
    }
}
