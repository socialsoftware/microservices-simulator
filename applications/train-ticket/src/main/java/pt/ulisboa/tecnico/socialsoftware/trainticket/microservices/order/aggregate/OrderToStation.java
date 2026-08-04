package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderToStationDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.StationDto;

@Entity
public class OrderToStation {
    @Id
    @GeneratedValue
    private Long id;
    private String toName;
    private Integer stationAggregateId;
    private Integer stationVersion;
    private AggregateState stationState;
    @OneToOne
    private Order order;

    public OrderToStation() {

    }

    public OrderToStation(StationDto stationDto) {
        setStationAggregateId(stationDto.getAggregateId());
        setStationVersion(stationDto.getVersion());
        setStationState(stationDto.getState());
    }

    public OrderToStation(OrderToStationDto orderToStationDto) {
        setToName(orderToStationDto.getName());
        setStationAggregateId(orderToStationDto.getAggregateId());
        setStationVersion(orderToStationDto.getVersion());
        setStationState(orderToStationDto.getState() != null ? AggregateState.valueOf(orderToStationDto.getState()) : null);
    }

    public OrderToStation(OrderToStation other) {
        setToName(other.getToName());
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

    public String getToName() {
        return toName;
    }

    public void setToName(String toName) {
        this.toName = toName;
    }

    public Integer getStationAggregateId() {
        return stationAggregateId;
    }

    public void setStationAggregateId(Integer stationAggregateId) {
        this.stationAggregateId = stationAggregateId;
    }

    public Integer getStationVersion() {
        return stationVersion;
    }

    public void setStationVersion(Integer stationVersion) {
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




    public OrderToStationDto buildDto() {
        OrderToStationDto dto = new OrderToStationDto();
        dto.setName(getToName());
        dto.setAggregateId(getStationAggregateId());
        dto.setVersion(getStationVersion());
        dto.setState(getStationState() != null ? getStationState().name() : null);
        return dto;
    }
}