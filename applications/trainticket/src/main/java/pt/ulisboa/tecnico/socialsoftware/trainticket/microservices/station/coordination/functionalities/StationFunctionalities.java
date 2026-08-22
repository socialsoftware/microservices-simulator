package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.CreateStationFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.DeleteStationFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.GetStationByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.GetStationsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.UpdateStationFunctionalitySagas;

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

    public StationDto createStation(StationDto stationDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateStationFunctionalitySagas saga = new CreateStationFunctionalitySagas(
                unitOfWorkService, stationDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedStationDto();
    }

    public void updateStation(Integer stationAggregateId, StationDto stationDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateStationFunctionalitySagas saga = new UpdateStationFunctionalitySagas(
                unitOfWorkService, stationAggregateId, stationDto, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteStation(Integer stationAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteStationFunctionalitySagas saga = new DeleteStationFunctionalitySagas(
                unitOfWorkService, stationAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }
}
