package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.service.TripService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.coordination.webapi.requestDtos.CreateTripRequestDto;
import java.util.List;

@Service
public class TripFunctionalities {
    @Autowired
    private TripService tripService;

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

    public TripDto createTrip(CreateTripRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateTripFunctionalitySagas createTripFunctionalitySagas = new CreateTripFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createTripFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTripFunctionalitySagas.getCreatedTripDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TripDto getTripById(Integer tripAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTripByIdFunctionalitySagas getTripByIdFunctionalitySagas = new GetTripByIdFunctionalitySagas(
                        sagaUnitOfWorkService, tripAggregateId, sagaUnitOfWork, commandGateway);
                getTripByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTripByIdFunctionalitySagas.getTripDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TripDto updateTrip(TripDto tripDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(tripDto);
                UpdateTripFunctionalitySagas updateTripFunctionalitySagas = new UpdateTripFunctionalitySagas(
                        sagaUnitOfWorkService, tripDto, sagaUnitOfWork, commandGateway);
                updateTripFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTripFunctionalitySagas.getUpdatedTripDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTrip(Integer tripAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTripFunctionalitySagas deleteTripFunctionalitySagas = new DeleteTripFunctionalitySagas(
                        sagaUnitOfWorkService, tripAggregateId, sagaUnitOfWork, commandGateway);
                deleteTripFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TripDto> getAllTrips() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllTripsFunctionalitySagas getAllTripsFunctionalitySagas = new GetAllTripsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllTripsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllTripsFunctionalitySagas.getTrips();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TripDto tripDto) {
        if (tripDto.getTripNumber() == null) {
            throw new TrainTicketException(TRIP_MISSING_TRIPNUMBER);
        }
        if (tripDto.getStartTime() == null) {
            throw new TrainTicketException(TRIP_MISSING_STARTTIME);
        }
        if (tripDto.getEndTime() == null) {
            throw new TrainTicketException(TRIP_MISSING_ENDTIME);
        }
}

    private void checkInput(CreateTripRequestDto createRequest) {
        if (createRequest.getTripNumber() == null) {
            throw new TrainTicketException(TRIP_MISSING_TRIPNUMBER);
        }
        if (createRequest.getStartTime() == null) {
            throw new TrainTicketException(TRIP_MISSING_STARTTIME);
        }
        if (createRequest.getEndTime() == null) {
            throw new TrainTicketException(TRIP_MISSING_ENDTIME);
        }
}
}