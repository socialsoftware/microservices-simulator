package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.exception.TrainTicketException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.service.StationService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.coordination.webapi.requestDtos.CreateStationRequestDto;
import java.util.List;

@Service
public class StationFunctionalities {
    @Autowired
    private StationService stationService;

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
            throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public StationDto createStation(CreateStationRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateStationFunctionalitySagas createStationFunctionalitySagas = new CreateStationFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createStationFunctionalitySagas.getCreatedStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public StationDto getStationById(Integer stationAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetStationByIdFunctionalitySagas getStationByIdFunctionalitySagas = new GetStationByIdFunctionalitySagas(
                        sagaUnitOfWorkService, stationAggregateId, sagaUnitOfWork, commandGateway);
                getStationByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getStationByIdFunctionalitySagas.getStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public StationDto updateStation(StationDto stationDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(stationDto);
                UpdateStationFunctionalitySagas updateStationFunctionalitySagas = new UpdateStationFunctionalitySagas(
                        sagaUnitOfWorkService, stationDto, sagaUnitOfWork, commandGateway);
                updateStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateStationFunctionalitySagas.getUpdatedStationDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteStation(Integer stationAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteStationFunctionalitySagas deleteStationFunctionalitySagas = new DeleteStationFunctionalitySagas(
                        sagaUnitOfWorkService, stationAggregateId, sagaUnitOfWork, commandGateway);
                deleteStationFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<StationDto> getAllStations() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllStationsFunctionalitySagas getAllStationsFunctionalitySagas = new GetAllStationsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllStationsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllStationsFunctionalitySagas.getStations();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(StationDto stationDto) {
        if (stationDto.getName() == null) {
            throw new TrainTicketException(STATION_MISSING_NAME);
        }
}

    private void checkInput(CreateStationRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TrainTicketException(STATION_MISSING_NAME);
        }
}
}