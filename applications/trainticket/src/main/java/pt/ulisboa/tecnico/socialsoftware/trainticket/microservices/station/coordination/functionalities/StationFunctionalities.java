package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.GetStationByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.GetStationsFunctionalitySagas;

import java.util.List;

@Service
public class StationFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;

    public StationDto getStationById(Integer stationAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getStationById");
        GetStationByIdFunctionalitySagas saga = new GetStationByIdFunctionalitySagas(
                unitOfWorkService, stationAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getStationDto();
    }

    public List<StationDto> getStations() {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getStations");
        GetStationsFunctionalitySagas saga = new GetStationsFunctionalitySagas(
                unitOfWorkService, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getStations();
    }
}
