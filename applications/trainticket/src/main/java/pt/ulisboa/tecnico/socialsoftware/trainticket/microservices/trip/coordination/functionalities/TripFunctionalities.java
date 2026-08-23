package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas.GetTripByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas.GetTripsFunctionalitySagas;

import java.util.List;

@Service
public class TripFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public TripDto getTripById(Integer tripAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTripById");
        GetTripByIdFunctionalitySagas saga = new GetTripByIdFunctionalitySagas(
                unitOfWorkService, tripAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTripDto();
    }

    public List<TripDto> getTrips() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTrips");
        GetTripsFunctionalitySagas saga = new GetTripsFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTrips();
    }
}
