package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.CreateTrainTypeFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.DeleteTrainTypeFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.GetTrainTypeByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.GetTrainTypesFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.coordination.sagas.UpdateTrainTypeFunctionalitySagas;

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

    public TrainTypeDto createTrainType(TrainTypeDto trainTypeDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateTrainTypeFunctionalitySagas saga = new CreateTrainTypeFunctionalitySagas(
                unitOfWorkService, trainTypeDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedTrainTypeDto();
    }

    public void updateTrainType(Integer trainTypeAggregateId, TrainTypeDto trainTypeDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateTrainTypeFunctionalitySagas saga = new UpdateTrainTypeFunctionalitySagas(
                unitOfWorkService, trainTypeAggregateId, trainTypeDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteTrainType(Integer trainTypeAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteTrainTypeFunctionalitySagas saga = new DeleteTrainTypeFunctionalitySagas(
                unitOfWorkService, trainTypeAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
