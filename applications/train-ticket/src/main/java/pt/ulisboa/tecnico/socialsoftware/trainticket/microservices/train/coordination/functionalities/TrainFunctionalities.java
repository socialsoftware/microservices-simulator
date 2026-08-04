package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.service.TrainService;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.coordination.webapi.requestDtos.CreateTrainRequestDto;
import java.util.List;

@Service
public class TrainFunctionalities {
    @Autowired
    private TrainService trainService;

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

    public TrainDto createTrain(CreateTrainRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateTrainFunctionalitySagas createTrainFunctionalitySagas = new CreateTrainFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createTrainFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTrainFunctionalitySagas.getCreatedTrainDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TrainDto getTrainById(Integer trainAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTrainByIdFunctionalitySagas getTrainByIdFunctionalitySagas = new GetTrainByIdFunctionalitySagas(
                        sagaUnitOfWorkService, trainAggregateId, sagaUnitOfWork, commandGateway);
                getTrainByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTrainByIdFunctionalitySagas.getTrainDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TrainDto updateTrain(TrainDto trainDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(trainDto);
                UpdateTrainFunctionalitySagas updateTrainFunctionalitySagas = new UpdateTrainFunctionalitySagas(
                        sagaUnitOfWorkService, trainDto, sagaUnitOfWork, commandGateway);
                updateTrainFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateTrainFunctionalitySagas.getUpdatedTrainDto();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteTrain(Integer trainAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteTrainFunctionalitySagas deleteTrainFunctionalitySagas = new DeleteTrainFunctionalitySagas(
                        sagaUnitOfWorkService, trainAggregateId, sagaUnitOfWork, commandGateway);
                deleteTrainFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TrainDto> getAllTrains() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllTrainsFunctionalitySagas getAllTrainsFunctionalitySagas = new GetAllTrainsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllTrainsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllTrainsFunctionalitySagas.getTrains();
            default: throw new TrainTicketException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(TrainDto trainDto) {
        if (trainDto.getName() == null) {
            throw new TrainTicketException(TRAIN_MISSING_NAME);
        }
}

    private void checkInput(CreateTrainRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TrainTicketException(TRAIN_MISSING_NAME);
        }
}
}