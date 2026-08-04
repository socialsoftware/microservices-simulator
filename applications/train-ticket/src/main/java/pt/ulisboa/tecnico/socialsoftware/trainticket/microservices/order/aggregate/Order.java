package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;
import pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorException;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe.OrderSubscribesContactsDeletedOrderContactsExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe.OrderSubscribesStationDeletedOrderStationsExist;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe.OrderSubscribesTrainDeletedOrderTraintypeExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe.OrderSubscribesTripDeletedOrderTripExists;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.events.subscribe.OrderSubscribesUserDeletedOrderUserExists;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderContactsDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderFromStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderToStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderUserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.OrderStatus;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.enums.SeatClass;

import static pt.ulisboa.tecnico.socialsoftware.ms.exception.SimulatorErrorMessage.INVARIANT_BREAK;

@Entity
public abstract class Order extends Aggregate {
    private String boughtDate;
    private String travelDate;
    private String travelTime;
    private Integer coachNumber;
    @Enumerated(EnumType.STRING)
    private SeatClass seatClass;
    private String seatNumber;
    private Double price;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderUser user;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderContacts contacts;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderTrip trip;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderTrain train;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderFromStation fromStation;
    @OneToOne(cascade = CascadeType.ALL, mappedBy = "order")
    private OrderToStation toStation;

    public Order() {

    }

    public Order(Integer aggregateId, OrderDto orderDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setBoughtDate(orderDto.getBoughtDate());
        setTravelDate(orderDto.getTravelDate());
        setTravelTime(orderDto.getTravelTime());
        setCoachNumber(orderDto.getCoachNumber());
        setSeatClass(SeatClass.valueOf(orderDto.getSeatClass()));
        setSeatNumber(orderDto.getSeatNumber());
        setPrice(orderDto.getPrice());
        setStatus(OrderStatus.valueOf(orderDto.getStatus()));
        setUser(orderDto.getUser() != null ? new OrderUser(orderDto.getUser()) : null);
        setContacts(orderDto.getContacts() != null ? new OrderContacts(orderDto.getContacts()) : null);
        setTrip(orderDto.getTrip() != null ? new OrderTrip(orderDto.getTrip()) : null);
        setTrain(orderDto.getTrain() != null ? new OrderTrain(orderDto.getTrain()) : null);
        setFromStation(orderDto.getFromStation() != null ? new OrderFromStation(orderDto.getFromStation()) : null);
        setToStation(orderDto.getToStation() != null ? new OrderToStation(orderDto.getToStation()) : null);
    }


    public Order(Order other) {
        super(other);
        setBoughtDate(other.getBoughtDate());
        setTravelDate(other.getTravelDate());
        setTravelTime(other.getTravelTime());
        setCoachNumber(other.getCoachNumber());
        setSeatClass(other.getSeatClass());
        setSeatNumber(other.getSeatNumber());
        setPrice(other.getPrice());
        setStatus(other.getStatus());
        setUser(new OrderUser(other.getUser()));
        setContacts(new OrderContacts(other.getContacts()));
        setTrip(new OrderTrip(other.getTrip()));
        setTrain(new OrderTrain(other.getTrain()));
        setFromStation(new OrderFromStation(other.getFromStation()));
        setToStation(new OrderToStation(other.getToStation()));
    }

    public String getBoughtDate() {
        return boughtDate;
    }

    public void setBoughtDate(String boughtDate) {
        this.boughtDate = boughtDate;
    }

    public String getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(String travelDate) {
        this.travelDate = travelDate;
    }

    public String getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(String travelTime) {
        this.travelTime = travelTime;
    }

    public Integer getCoachNumber() {
        return coachNumber;
    }

    public void setCoachNumber(Integer coachNumber) {
        this.coachNumber = coachNumber;
    }

    public SeatClass getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(SeatClass seatClass) {
        this.seatClass = seatClass;
    }

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public Double getPrice() {
        return price;
    }

    public void setPrice(Double price) {
        this.price = price;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public OrderUser getUser() {
        return user;
    }

    public void setUser(OrderUser user) {
        this.user = user;
        if (this.user != null) {
            this.user.setOrder(this);
        }
    }

    public OrderContacts getContacts() {
        return contacts;
    }

    public void setContacts(OrderContacts contacts) {
        this.contacts = contacts;
        if (this.contacts != null) {
            this.contacts.setOrder(this);
        }
    }

    public OrderTrip getTrip() {
        return trip;
    }

    public void setTrip(OrderTrip trip) {
        this.trip = trip;
        if (this.trip != null) {
            this.trip.setOrder(this);
        }
    }

    public OrderTrain getTrain() {
        return train;
    }

    public void setTrain(OrderTrain train) {
        this.train = train;
        if (this.train != null) {
            this.train.setOrder(this);
        }
    }

    public OrderFromStation getFromStation() {
        return fromStation;
    }

    public void setFromStation(OrderFromStation fromStation) {
        this.fromStation = fromStation;
        if (this.fromStation != null) {
            this.fromStation.setOrder(this);
        }
    }

    public OrderToStation getToStation() {
        return toStation;
    }

    public void setToStation(OrderToStation toStation) {
        this.toStation = toStation;
        if (this.toStation != null) {
            this.toStation.setOrder(this);
        }
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        Set<EventSubscription> eventSubscriptions = new HashSet<>();
        if (this.getState() == AggregateState.ACTIVE) {
            interInvariantOrderUserExists(eventSubscriptions);
            interInvariantOrderContactsExists(eventSubscriptions);
            interInvariantOrderTripExists(eventSubscriptions);
            interInvariantOrderTraintypeExists(eventSubscriptions);
            interInvariantOrderStationsExist(eventSubscriptions);
        }
        return eventSubscriptions;
    }
    private void interInvariantOrderUserExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesUserDeletedOrderUserExists(this.getUser()));
    }

    private void interInvariantOrderContactsExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesContactsDeletedOrderContactsExists(this.getContacts()));
    }

    private void interInvariantOrderTripExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesTripDeletedOrderTripExists(this.getTrip()));
    }

    private void interInvariantOrderTraintypeExists(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesTrainDeletedOrderTraintypeExists(this.getTrain()));
    }

    private void interInvariantOrderStationsExist(Set<EventSubscription> eventSubscriptions) {
        eventSubscriptions.add(new OrderSubscribesStationDeletedOrderStationsExist(this.getFromStation()));
    }


    private boolean invariantSeatClassSet() {
        return this.seatClass != null;
    }

    private boolean invariantCoachNumberPositive() {
        return coachNumber > 0;
    }

    private boolean invariantStatusSet() {
        return this.status != null;
    }

    private boolean invariantPriceNotNegative() {
        return price >= 0;
    }

    private boolean invariantUserSet() {
        return this.user != null;
    }

    private boolean invariantTripSet() {
        return this.trip != null;
    }
    @Override
    public void verifyInvariants() {
        if (!invariantSeatClassSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must have a seat class");
        }
        if (!invariantCoachNumberPositive()) {
            throw new SimulatorException(INVARIANT_BREAK, "Coach number must be positive");
        }
        if (!invariantStatusSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must have a status");
        }
        if (!invariantPriceNotNegative()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order price cannot be negative");
        }
        if (!invariantUserSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must belong to a user account");
        }
        if (!invariantTripSet()) {
            throw new SimulatorException(INVARIANT_BREAK, "Order must reference a trip");
        }
    }

    public OrderDto buildDto() {
        OrderDto dto = new OrderDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setBoughtDate(getBoughtDate());
        dto.setTravelDate(getTravelDate());
        dto.setTravelTime(getTravelTime());
        dto.setCoachNumber(getCoachNumber());
        dto.setSeatClass(getSeatClass() != null ? getSeatClass().name() : null);
        dto.setSeatNumber(getSeatNumber());
        dto.setPrice(getPrice());
        dto.setStatus(getStatus() != null ? getStatus().name() : null);
        dto.setUser(getUser() != null ? new OrderUserDto(getUser()) : null);
        dto.setContacts(getContacts() != null ? new OrderContactsDto(getContacts()) : null);
        dto.setTrip(getTrip() != null ? new OrderTripDto(getTrip()) : null);
        dto.setTrain(getTrain() != null ? new OrderTrainDto(getTrain()) : null);
        dto.setFromStation(getFromStation() != null ? new OrderFromStationDto(getFromStation()) : null);
        dto.setToStation(getToStation() != null ? new OrderToStationDto(getToStation()) : null);
        return dto;
    }
}