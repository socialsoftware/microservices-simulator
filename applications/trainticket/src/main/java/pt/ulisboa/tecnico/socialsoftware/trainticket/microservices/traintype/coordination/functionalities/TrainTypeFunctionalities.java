package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.GetTrainTypeByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.GetTrainTypesFunctionalitySagas;

import java.util.List;

@Service
public class TrainTypeFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public TrainTypeDto getTrainTypeById(Integer trainTypeAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTrainTypeById");
        GetTrainTypeByIdFunctionalitySagas saga = new GetTrainTypeByIdFunctionalitySagas(
                unitOfWorkService, trainTypeAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTrainTypeDto();
    }

    public List<TrainTypeDto> getTrainTypes() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTrainTypes");
        GetTrainTypesFunctionalitySagas saga = new GetTrainTypesFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTrainTypes();
    }
}
