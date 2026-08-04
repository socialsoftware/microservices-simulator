package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.order.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.OrderTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

@Entity
public class OrderTrain {
    @Id
    @GeneratedValue
    private Long id;
    private String trainNumber;
    private Integer trainAggregateId;
    private Integer trainVersion;
    private AggregateState trainState;
    @OneToOne
    private Order order;

    public OrderTrain() {

    }

    public OrderTrain(TrainDto trainDto) {
        setTrainAggregateId(trainDto.getAggregateId());
        setTrainVersion(trainDto.getVersion());
        setTrainState(trainDto.getState());
    }

    public OrderTrain(OrderTrainDto orderTrainDto) {
        setTrainNumber(orderTrainDto.getName());
        setTrainAggregateId(orderTrainDto.getAggregateId());
        setTrainVersion(orderTrainDto.getVersion());
        setTrainState(orderTrainDto.getState() != null ? AggregateState.valueOf(orderTrainDto.getState()) : null);
    }

    public OrderTrain(OrderTrain other) {
        setTrainNumber(other.getTrainNumber());
        setTrainAggregateId(other.getTrainAggregateId());
        setTrainVersion(other.getTrainVersion());
        setTrainState(other.getTrainState());
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTrainNumber() {
        return trainNumber;
    }

    public void setTrainNumber(String trainNumber) {
        this.trainNumber = trainNumber;
    }

    public Integer getTrainAggregateId() {
        return trainAggregateId;
    }

    public void setTrainAggregateId(Integer trainAggregateId) {
        this.trainAggregateId = trainAggregateId;
    }

    public Integer getTrainVersion() {
        return trainVersion;
    }

    public void setTrainVersion(Integer trainVersion) {
        this.trainVersion = trainVersion;
    }

    public AggregateState getTrainState() {
        return trainState;
    }

    public void setTrainState(AggregateState trainState) {
        this.trainState = trainState;
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }




    public OrderTrainDto buildDto() {
        OrderTrainDto dto = new OrderTrainDto();
        dto.setName(getTrainNumber());
        dto.setAggregateId(getTrainAggregateId());
        dto.setVersion(getTrainVersion());
        dto.setState(getTrainState() != null ? getTrainState().name() : null);
        return dto;
    }
}