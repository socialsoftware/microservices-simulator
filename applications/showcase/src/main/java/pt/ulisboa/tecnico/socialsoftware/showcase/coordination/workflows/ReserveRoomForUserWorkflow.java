package pt.ulisboa.tecnico.socialsoftware.showcase.coordination.workflows;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingRoom;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.aggregate.BookingUser;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.booking.service.BookingService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.service.RoomService;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.BookingDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.sagaStates.ReservationSagaStates;

@Component
public class ReserveRoomForUserWorkflow extends WorkflowFunctionality {

    @Autowired private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired private CommandGateway commandGateway;
    @Autowired private BookingService bookingService;
    @Autowired private RoomService roomService;
    @Autowired private UserService userService;

    private UserDto createUser;
    private RoomDto loadRoom;
    private BookingDto bookRoom;


    public void execute(String username, String email, Integer roomId, String checkIn, String checkOut, Integer nights, Double price) {
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork("ReserveRoomForUser");
        this.workflow = new SagaWorkflow(this, sagaUnitOfWorkService, unitOfWork);

        java.util.ArrayList<pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.FlowStep> prevDeps;
        SagaStep createUserStep = new SagaStep("createUser", () -> {
            this.createUser = userService.signUp(username, email, unitOfWork);
        });
        createUserStep.registerCompensation(() -> {
            userService.deleteUser(this.createUser.getAggregateId(), unitOfWork);
        }, unitOfWork);
        workflow.addStep(createUserStep);

        SagaStep loadRoomStep = new SagaStep("loadRoom", () -> {
            this.loadRoom = roomService.getRoomById(roomId, unitOfWork);
        });
        prevDeps = new java.util.ArrayList<>();
        prevDeps.add(createUserStep);
        loadRoomStep.setDependencies(prevDeps);
        workflow.addStep(loadRoomStep);

        SagaStep reserveRoomStep = new SagaStep("reserveRoom", () -> {
            sagaUnitOfWorkService.verifySagaState(roomId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(ReservationSagaStates.IN_BOOK_ROOM, ReservationSagaStates.IN_CANCEL_BOOKING)));
            sagaUnitOfWorkService.registerSagaState(roomId, ReservationSagaStates.IN_BOOK_ROOM, unitOfWork);
            roomService.reserve(roomId, unitOfWork);
        });
        prevDeps = new java.util.ArrayList<>();
        prevDeps.add(loadRoomStep);
        reserveRoomStep.setDependencies(prevDeps);
        reserveRoomStep.registerCompensation(() -> {
            roomService.release(roomId, unitOfWork);
        }, unitOfWork);
        workflow.addStep(reserveRoomStep);

        SagaStep bookRoomStep = new SagaStep("bookRoom", () -> {
            this.bookRoom = bookingService.bookRoom(new BookingUser(this.createUser), new BookingRoom(this.loadRoom), checkIn, checkOut, nights, price, unitOfWork);
        });
        prevDeps = new java.util.ArrayList<>();
        prevDeps.add(reserveRoomStep);
        bookRoomStep.setDependencies(prevDeps);
        workflow.addStep(bookRoomStep);

        this.executeWorkflow(unitOfWork);
    }
}
