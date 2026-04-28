package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.exception.ShowcaseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomAmenityDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.coordination.webapi.requestDtos.CreateRoomRequestDto;
import java.util.List;

@Service
public class RoomFunctionalities {
    @Autowired
    private RoomService roomService;

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
            throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomDto createRoom(CreateRoomRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateRoomFunctionalitySagas createRoomFunctionalitySagas = new CreateRoomFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createRoomFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createRoomFunctionalitySagas.getCreatedRoomDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomDto getRoomById(Integer roomAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetRoomByIdFunctionalitySagas getRoomByIdFunctionalitySagas = new GetRoomByIdFunctionalitySagas(
                        sagaUnitOfWorkService, roomAggregateId, sagaUnitOfWork, commandGateway);
                getRoomByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getRoomByIdFunctionalitySagas.getRoomDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomDto updateRoom(RoomDto roomDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(roomDto);
                UpdateRoomFunctionalitySagas updateRoomFunctionalitySagas = new UpdateRoomFunctionalitySagas(
                        sagaUnitOfWorkService, roomDto, sagaUnitOfWork, commandGateway);
                updateRoomFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateRoomFunctionalitySagas.getUpdatedRoomDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteRoom(Integer roomAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteRoomFunctionalitySagas deleteRoomFunctionalitySagas = new DeleteRoomFunctionalitySagas(
                        sagaUnitOfWorkService, roomAggregateId, sagaUnitOfWork, commandGateway);
                deleteRoomFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<RoomDto> getAllRooms() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllRoomsFunctionalitySagas getAllRoomsFunctionalitySagas = new GetAllRoomsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllRoomsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllRoomsFunctionalitySagas.getRooms();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomAmenityDto addRoomAmenitie(Integer roomId, Integer code, RoomAmenityDto amenitieDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddRoomAmenitieFunctionalitySagas addRoomAmenitieFunctionalitySagas = new AddRoomAmenitieFunctionalitySagas(
                        sagaUnitOfWorkService,
                        roomId, code, amenitieDto,
                        sagaUnitOfWork, commandGateway);
                addRoomAmenitieFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addRoomAmenitieFunctionalitySagas.getAddedAmenitieDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<RoomAmenityDto> addRoomAmenities(Integer roomId, List<RoomAmenityDto> amenitieDtos) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddRoomAmenitiesFunctionalitySagas addRoomAmenitiesFunctionalitySagas = new AddRoomAmenitiesFunctionalitySagas(
                        sagaUnitOfWorkService,
                        roomId, amenitieDtos,
                        sagaUnitOfWork, commandGateway);
                addRoomAmenitiesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return addRoomAmenitiesFunctionalitySagas.getAddedAmenitieDtos();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomAmenityDto getRoomAmenitie(Integer roomId, Integer code) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetRoomAmenitieFunctionalitySagas getRoomAmenitieFunctionalitySagas = new GetRoomAmenitieFunctionalitySagas(
                        sagaUnitOfWorkService,
                        roomId, code,
                        sagaUnitOfWork, commandGateway);
                getRoomAmenitieFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getRoomAmenitieFunctionalitySagas.getAmenitieDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public RoomAmenityDto updateRoomAmenitie(Integer roomId, Integer code, RoomAmenityDto amenitieDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateRoomAmenitieFunctionalitySagas updateRoomAmenitieFunctionalitySagas = new UpdateRoomAmenitieFunctionalitySagas(
                        sagaUnitOfWorkService,
                        roomId, code, amenitieDto,
                        sagaUnitOfWork, commandGateway);
                updateRoomAmenitieFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateRoomAmenitieFunctionalitySagas.getUpdatedAmenitieDto();
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeRoomAmenitie(Integer roomId, Integer code) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveRoomAmenitieFunctionalitySagas removeRoomAmenitieFunctionalitySagas = new RemoveRoomAmenitieFunctionalitySagas(
                        sagaUnitOfWorkService,
                        roomId, code,
                        sagaUnitOfWork, commandGateway);
                removeRoomAmenitieFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void reserve(Integer roomId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ReserveFunctionalitySagas reserveFunctionalitySagas = new ReserveFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, sagaUnitOfWork, commandGateway);
                reserveFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void checkIn(Integer roomId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CheckInFunctionalitySagas checkInFunctionalitySagas = new CheckInFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, sagaUnitOfWork, commandGateway);
                checkInFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void checkOut(Integer roomId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CheckOutFunctionalitySagas checkOutFunctionalitySagas = new CheckOutFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, sagaUnitOfWork, commandGateway);
                checkOutFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void release(Integer roomId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                ReleaseFunctionalitySagas releaseFunctionalitySagas = new ReleaseFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, sagaUnitOfWork, commandGateway);
                releaseFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void retire(Integer roomId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RetireFunctionalitySagas retireFunctionalitySagas = new RetireFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, sagaUnitOfWork, commandGateway);
                retireFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void renameAmenity(Integer roomId, Integer amenityCode, String newName) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RenameAmenityFunctionalitySagas renameAmenityFunctionalitySagas = new RenameAmenityFunctionalitySagas(
                        sagaUnitOfWorkService, roomService, roomId, amenityCode, newName, sagaUnitOfWork, commandGateway);
                renameAmenityFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new ShowcaseException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(RoomDto roomDto) {
        if (roomDto.getRoomNumber() == null) {
            throw new ShowcaseException(ROOM_MISSING_ROOMNUMBER);
        }
        if (roomDto.getDescription() == null) {
            throw new ShowcaseException(ROOM_MISSING_DESCRIPTION);
        }
}

    private void checkInput(CreateRoomRequestDto createRequest) {
        if (createRequest.getRoomNumber() == null) {
            throw new ShowcaseException(ROOM_MISSING_ROOMNUMBER);
        }
        if (createRequest.getDescription() == null) {
            throw new ShowcaseException(ROOM_MISSING_DESCRIPTION);
        }
}
}