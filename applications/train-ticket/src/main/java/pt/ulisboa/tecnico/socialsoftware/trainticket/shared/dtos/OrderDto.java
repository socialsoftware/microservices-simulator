package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.Order;

public class OrderDto implements Serializable {
    private Integer aggregateId;
    private Long version;
    private AggregateState state;
    private String boughtDate;
    private String travelDate;
    private String travelTime;
    private Integer coachNumber;
    private String seatClass;
    private String seatNumber;
    private Double price;
    private String status;
    private OrderUserDto user;
    private OrderContactsDto contacts;
    private OrderTripDto trip;
    private OrderTrainDto train;
    private OrderFromStationDto fromStation;
    private OrderToStationDto toStation;

    public OrderDto() {
    }

    public OrderDto(Order order) {
        this.aggregateId = order.getAggregateId();
        this.version = order.getVersion();
        this.state = order.getState();
        this.boughtDate = order.getBoughtDate();
        this.travelDate = order.getTravelDate();
        this.travelTime = order.getTravelTime();
        this.coachNumber = order.getCoachNumber();
        this.seatClass = order.getSeatClass() != null ? order.getSeatClass().name() : null;
        this.seatNumber = order.getSeatNumber();
        this.price = order.getPrice();
        this.status = order.getStatus() != null ? order.getStatus().name() : null;
        this.user = order.getUser() != null ? new OrderUserDto(order.getUser()) : null;
        this.contacts = order.getContacts() != null ? new OrderContactsDto(order.getContacts()) : null;
        this.trip = order.getTrip() != null ? new OrderTripDto(order.getTrip()) : null;
        this.train = order.getTrain() != null ? new OrderTrainDto(order.getTrain()) : null;
        this.fromStation = order.getFromStation() != null ? new OrderFromStationDto(order.getFromStation()) : null;
        this.toStation = order.getToStation() != null ? new OrderToStationDto(order.getToStation()) : null;
    }

    public Integer getAggregateId() {
        return aggregateId;
    }

    public void setAggregateId(Integer aggregateId) {
        this.aggregateId = aggregateId;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public AggregateState getState() {
        return state;
    }

    public void setState(AggregateState state) {
        this.state = state;
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

    public String getSeatClass() {
        return seatClass;
    }

    public void setSeatClass(String seatClass) {
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public OrderUserDto getUser() {
        return user;
    }

    public void setUser(OrderUserDto user) {
        this.user = user;
    }

    public OrderContactsDto getContacts() {
        return contacts;
    }

    public void setContacts(OrderContactsDto contacts) {
        this.contacts = contacts;
    }

    public OrderTripDto getTrip() {
        return trip;
    }

    public void setTrip(OrderTripDto trip) {
        this.trip = trip;
    }

    public OrderTrainDto getTrain() {
        return train;
    }

    public void setTrain(OrderTrainDto train) {
        this.train = train;
    }

    public OrderFromStationDto getFromStation() {
        return fromStation;
    }

    public void setFromStation(OrderFromStationDto fromStation) {
        this.fromStation = fromStation;
    }

    public OrderToStationDto getToStation() {
        return toStation;
    }

    public void setToStation(OrderToStationDto toStation) {
        this.toStation = toStation;
    }
}