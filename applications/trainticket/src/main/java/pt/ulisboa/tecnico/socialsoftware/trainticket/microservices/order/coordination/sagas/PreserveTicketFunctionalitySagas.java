package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.trainticket.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.contacts.GetContactsByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order.PreserveTicketCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.priceconfig.GetPriceConfigByRouteAndTrainTypeCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.route.GetRouteByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.traintype.GetTrainTypeByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.commands.trip.GetTripByIdCommand;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.SeatClass;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

import java.util.ArrayList;
import java.util.Arrays;

// PreserveTicket creates the order it books, so no step takes a semantic lock: there is no prior
// state to guard and no aggregate to release. The create step is also the last one, so the unit of
// work owns the abort and no compensation is registered.
public class PreserveTicketFunctionalitySagas extends WorkflowFunctionality {
    private ContactsDto contactsDto;
    private TripDto tripDto;
    private RouteDto routeDto;
    private TrainTypeDto trainTypeDto;
    private PriceConfigDto priceConfigDto;
    private OrderDto createdOrderDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;

    public PreserveTicketFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService,
                                            OrderDto requestedOrderDto,
                                            SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        buildWorkflow(requestedOrderDto, unitOfWork);
    }

    public void buildWorkflow(OrderDto requestedOrderDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getContactsStep = new SagaStep("getContactsStep", () -> {
            // [P4a] CONTACTS_EXIST - the fetch throwing is the enforcement. The DTO it returns also
            // feeds the CONTACTS_BELONG_TO_ACCOUNT guard in OrderService.
            GetContactsByIdCommand cmd = new GetContactsByIdCommand(
                    unitOfWork, ServiceMapping.CONTACTS.getServiceName(),
                    requestedOrderDto.getContactsAggregateId());
            this.contactsDto = (ContactsDto) commandGateway.send(cmd);
        });

        SagaStep getTripStep = new SagaStep("getTripStep", () -> {
            // [P4a] TRIP_EXISTS - the fetch throwing is the enforcement. The trip also names the
            // route, train type, trip number and time of day the order is booked against.
            GetTripByIdCommand cmd = new GetTripByIdCommand(
                    unitOfWork, ServiceMapping.TRIP.getServiceName(),
                    requestedOrderDto.getTripAggregateId());
            this.tripDto = (TripDto) commandGateway.send(cmd);
        });

        SagaStep getRouteStep = new SagaStep("getRouteStep", () -> {
            GetRouteByIdCommand cmd = new GetRouteByIdCommand(
                    unitOfWork, ServiceMapping.ROUTE.getServiceName(), this.tripDto.getRouteAggregateId());
            this.routeDto = (RouteDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        SagaStep getTrainTypeStep = new SagaStep("getTrainTypeStep", () -> {
            GetTrainTypeByIdCommand cmd = new GetTrainTypeByIdCommand(
                    unitOfWork, ServiceMapping.TRAIN_TYPE.getServiceName(),
                    this.tripDto.getTrainTypeAggregateId());
            this.trainTypeDto = (TrainTypeDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        SagaStep getPriceConfigStep = new SagaStep("getPriceConfigStep", () -> {
            // [P4a] PRICE_CONFIG_EXISTS - the lookup throws when the pair has no tariff.
            GetPriceConfigByRouteAndTrainTypeCommand cmd = new GetPriceConfigByRouteAndTrainTypeCommand(
                    unitOfWork, ServiceMapping.PRICE_CONFIG.getServiceName(),
                    this.tripDto.getRouteAggregateId(), this.tripDto.getTrainTypeAggregateId());
            this.priceConfigDto = (PriceConfigDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getTripStep)));

        SagaStep preserveTicketStep = new SagaStep("preserveTicketStep", () -> {
            // [P4b] the seat capacity of the requested class reaches Order as a single scalar; the
            // order stores nothing of the train type it was read from.
            PreserveTicketCommand cmd = new PreserveTicketCommand(
                    unitOfWork, ServiceMapping.ORDER.getServiceName(), requestedOrderDto,
                    this.contactsDto, this.tripDto, this.routeDto, this.priceConfigDto,
                    capacityOf(this.trainTypeDto, requestedOrderDto.getSeatClass()));
            this.createdOrderDto = (OrderDto) commandGateway.send(cmd);
        }, new ArrayList<>(Arrays.asList(getContactsStep, getRouteStep, getTrainTypeStep, getPriceConfigStep)));

        this.workflow.addStep(getContactsStep);
        this.workflow.addStep(getTripStep);
        this.workflow.addStep(getRouteStep);
        this.workflow.addStep(getTrainTypeStep);
        this.workflow.addStep(getPriceConfigStep);
        this.workflow.addStep(preserveTicketStep);
    }

    private static Integer capacityOf(TrainTypeDto trainTypeDto, SeatClass seatClass) {
        return seatClass == SeatClass.FIRST_CLASS
                ? trainTypeDto.getFirstClassSeats()
                : trainTypeDto.getEconomyClassSeats();
    }

    public OrderDto getCreatedOrderDto() {
        return createdOrderDto;
    }
}
