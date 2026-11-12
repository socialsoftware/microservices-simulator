package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.GetLeftTicketCountCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;

public class GetLeftTicketCountFunctionalitySagas extends WorkflowFunctionality {
    private TripDto tripDto;
    private TrainTypeDto trainTypeDto;
    private Integer leftTicketCount;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public GetLeftTicketCountFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer tripAggregateId,
                                                LocalDate travelDate, SeatClass seatClass,
                                                SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(tripAggregateId, travelDate, seatClass, unitOfWork);
    }

    public void buildWorkflow(Integer tripAggregateId, LocalDate travelDate, SeatClass seatClass,
                              SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            GetTripByIdCommand cmd = new GetTripByIdCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(), tripAggregateId);
            this.tripDto = (TripDto) commandGateway.send(cmd);
        });

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            GetTrainTypeByIdCommand cmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(),
                    this.tripDto.getTrainTypeAggregateId());
            this.trainTypeDto = (TrainTypeDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        // [P4b] the seat capacity of the requested class reaches Order as a single scalar; Order
        // stores nothing of the train type it was computed from.
        SagaStep getLeftTicketCountStep = new SagaStep("getLeftTicketCountStep", () -> {
            GetLeftTicketCountCommand cmd = new GetLeftTicketCountCommand(
                    unitOfWork, ServiceMapping.ORDER.getServiceName(), tripAggregateId, travelDate, seatClass,
                    capacityOf(this.trainTypeDto, seatClass));
            this.leftTicketCount = (Integer) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTrainTypeStep)));

        this.workflow.addStep(getTripStep);
        this.workflow.addStep(getTrainTypeStep);
        this.workflow.addStep(getLeftTicketCountStep);
    }

    private static Integer capacityOf(TrainTypeDto trainTypeDto, SeatClass seatClass) {
        return seatClass == SeatClass.FIRST_CLASS
                ? trainTypeDto.getFirstClassSeats()
                : trainTypeDto.getEconomyClassSeats();
    }

    public Integer getLeftTicketCount() {
        return leftTicketCount;
    }
}
