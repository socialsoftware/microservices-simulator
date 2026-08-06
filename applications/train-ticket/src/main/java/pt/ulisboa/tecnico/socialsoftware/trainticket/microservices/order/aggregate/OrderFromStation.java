package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderFromStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

@Entity
public class OrderFromStation {
    @Id
    @GeneratedValue
    private Long id;
    private String fromName;
    private Integer stationAggregateId;
    private Long stationVersion;
    private AggregateState stationState;
    @OneToOne
    private Order order;

    public OrderFromStation() {

    }

    public OrderFromStation(StationDto stationDto) {
        setStationAggregateId(stationDto.getAggregateId());
        setStationVersion(stationDto.getVersion());
        setStationState(stationDto.getState());
    }

    public OrderFromStation(OrderFromStationDto orderFromStationDto) {
        setFromName(orderFromStationDto.getName());
        setStationAggregateId(orderFromStationDto.getAggregateId());
        setStationVersion(orderFromStationDto.getVersion());
        setStationState(orderFromStationDto.getState() != null ? AggregateState.valueOf(orderFromStationDto.getState()) : null);
    }

    public OrderFromStation(OrderFromStation other) {
        setFromName(other.getFromName());
        setStationAggregateId(other.getStationAggregateId());
        setStationVersion(other.getStationVersion());
        setStationState(other.getStationState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getFromName() {
        return fromName;
    }

    public void setFromName(String fromName) {
        this.fromName = fromName;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Long getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Long stationVersion) {
        this.stationVersion = stationVersion;
    }

    public AggregateState getStationState() {
        return stationState;
    }

    public void setStationState(AggregateState stationState) {
        this.stationState = stationState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderFromStationDto buildDto() {
        OrderFromStationDto dto = new OrderFromStationDto();
        dto.setName(getFromName());
        dto.setAggregateId(getStationAggregateId());
        dto.setVersion(getStationVersion());
        dto.setState(getStationState() != null ? getStationState().name() : null);
        return dto;
    }
}