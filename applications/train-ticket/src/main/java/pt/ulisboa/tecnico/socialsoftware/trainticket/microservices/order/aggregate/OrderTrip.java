package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTripDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;

@Entity
public class OrderTrip {
    @Id
    @GeneratedValue
    private Long id;
    private String tripNumber;
    private Integer tripAggregateId;
    private Long tripVersion;
    private AggregateState tripState;
    @OneToOne
    private Order order;

    public OrderTrip() {

    }

    public OrderTrip(TripDto tripDto) {
        setTripAggregateId(tripDto.getAggregateId());
        setTripVersion(tripDto.getVersion());
        setTripState(tripDto.getState());
    }

    public OrderTrip(OrderTripDto orderTripDto) {
        setTripNumber(orderTripDto.getTripNumber());
        setTripAggregateId(orderTripDto.getAggregateId());
        setTripVersion(orderTripDto.getVersion());
        setTripState(orderTripDto.getState() != null ? AggregateState.valueOf(orderTripDto.getState()) : null);
    }

    public OrderTrip(OrderTrip other) {
        setTripNumber(other.getTripNumber());
        setTripAggregateId(other.getTripAggregateId());
        setTripVersion(other.getTripVersion());
        setTripState(other.getTripState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
    }

    public Integer getTripAggregateId() {
        return tripAggregateId;
    }

    public void setTripAggregateId(Integer tripAggregateId) {
        this.tripAggregateId = tripAggregateId;
    }

    public Long getTripVersion() {
        return tripVersion;
    }

    public void setTripVersion(Long tripVersion) {
        this.tripVersion = tripVersion;
    }

    public AggregateState getTripState() {
        return tripState;
    }

    public void setTripState(AggregateState tripState) {
        this.tripState = tripState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderTripDto buildDto() {
        OrderTripDto dto = new OrderTripDto();
        dto.setTripNumber(getTripNumber());
        dto.setAggregateId(getTripAggregateId());
        dto.setVersion(getTripVersion());
        dto.setState(getTripState() != null ? getTripState().name() : null);
        return dto;
    }
}