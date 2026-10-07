package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.CreateStationFunctionalitySagas;

@Service 
public class StationFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    
    public StationDto createStation(StationDto stationDto) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createStation");
        CreateStationFunctionalitySagas saga = new CreateStationFunctionalitySagas(
                unitOfWorkService, stationDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getStationDto();
    }
}
