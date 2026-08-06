package pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos;

import java.io.Serializable;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate.OrderTrip;

public class OrderTripDto implements Serializable {
    private String tripNumber;
    private Integer aggregateId;
    private Long version;
    private String state;

    public OrderTripDto() {
    }

    public OrderTripDto(OrderTrip orderTrip) {
        this.tripNumber = orderTrip.getTripNumber();
        this.aggregateId = orderTrip.getTripAggregateId();
        this.version = orderTrip.getTripVersion();
        this.state = orderTrip.getTripState() != null ? orderTrip.getTripState().name() : null;
    }

    public String getTripNumber() {
        return tripNumber;
    }

    public void setTripNumber(String tripNumber) {
        this.tripNumber = tripNumber;
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

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }
}