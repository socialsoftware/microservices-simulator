package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripTrainDto;

@Entity
public class TripTrain {
    @Id
    @GeneratedValue
    private Long id;
    private String trainTypeName;
    private Integer trainAggregateId;
    private Integer trainVersion;
    private AggregateState trainState;
    @OneToOne
    private Trip trip;

    public TripTrain() {

    }

    public TripTrain(TrainDto trainDto) {
        setTrainAggregateId(trainDto.getAggregateId());
        setTrainVersion(trainDto.getVersion());
        setTrainState(trainDto.getState());
    }

    public TripTrain(TripTrainDto tripTrainDto) {
        setTrainTypeName(tripTrainDto.getName());
        setTrainAggregateId(tripTrainDto.getAggregateId());
        setTrainVersion(tripTrainDto.getVersion());
        setTrainState(tripTrainDto.getState() != null ? AggregateState.valueOf(tripTrainDto.getState()) : null);
    }

    public TripTrain(TripTrain other) {
        setTrainTypeName(other.getTrainTypeName());
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

    public String getTrainTypeName() {
        return trainTypeName;
    }

    public void setTrainTypeName(String trainTypeName) {
        this.trainTypeName = trainTypeName;
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

    public Trip getTrip() {
        return trip;
    }

    public void setTrip(Trip trip) {
        this.trip = trip;
    }




    public TripTrainDto buildDto() {
        TripTrainDto dto = new TripTrainDto();
        dto.setName(getTrainTypeName());
        dto.setAggregateId(getTrainAggregateId());
        dto.setVersion(getTrainVersion());
        dto.setState(getTrainState() != null ? getTrainState().name() : null);
        return dto;
    }
}