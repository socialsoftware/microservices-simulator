package pt.ulisboa.tecnico.socialsoftware.trainticket.commands.order;

import pt.ulisboa.tecnico.socialsoftware.ms.messaging.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.contacts.aggregate.ContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate.PriceConfigDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.route.aggregate.RouteDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.TripDto;

public class PreserveTicketCommand extends Command {
    private OrderDto orderDto;
    private ContactsDto contactsDto;
    private TripDto tripDto;
    private RouteDto routeDto;
    private PriceConfigDto priceConfigDto;
    private Integer capacity;

    public PreserveTicketCommand(UnitOfWork unitOfWork, String serviceName, OrderDto orderDto,
                                 ContactsDto contactsDto, TripDto tripDto, RouteDto routeDto,
                                 PriceConfigDto priceConfigDto, Integer capacity) {
        super(unitOfWork, serviceName, null);
        this.orderDto = orderDto;
        this.contactsDto = contactsDto;
        this.tripDto = tripDto;
        this.routeDto = routeDto;
        this.priceConfigDto = priceConfigDto;
        this.capacity = capacity;
    }

    public OrderDto getOrderDto() {
        return orderDto;
    }

    public ContactsDto getContactsDto() {
        return contactsDto;
    }

    public TripDto getTripDto() {
        return tripDto;
    }

    public RouteDto getRouteDto() {
        return routeDto;
    }

    public PriceConfigDto getPriceConfigDto() {
        return priceConfigDto;
    }

    public Integer getCapacity() {
        return capacity;
    }
}
