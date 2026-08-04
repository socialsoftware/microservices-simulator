package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.priceconfig.aggregate;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate.AggregateState;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.PriceConfigTrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

@Entity
public class PriceConfigTrain {
    @Id
    @GeneratedValue
    private Long id;
    private String trainTypeName;
    private Integer trainAggregateId;
    private Integer trainVersion;
    private AggregateState trainState;
    @OneToOne
    private PriceConfig priceconfig;

    public PriceConfigTrain() {

    }

    public PriceConfigTrain(TrainDto trainDto) {
        setTrainAggregateId(trainDto.getAggregateId());
        setTrainVersion(trainDto.getVersion());
        setTrainState(trainDto.getState());
    }

    public PriceConfigTrain(PriceConfigTrainDto priceConfigTrainDto) {
        setTrainTypeName(priceConfigTrainDto.getName());
        setTrainAggregateId(priceConfigTrainDto.getAggregateId());
        setTrainVersion(priceConfigTrainDto.getVersion());
        setTrainState(priceConfigTrainDto.getState() != null ? AggregateState.valueOf(priceConfigTrainDto.getState()) : null);
    }

    public PriceConfigTrain(PriceConfigTrain other) {
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

    public PriceConfig getPriceConfig() {
        return priceconfig;
    }

    public void setPriceConfig(PriceConfig priceconfig) {
        this.priceconfig = priceconfig;
    }




    public PriceConfigTrainDto buildDto() {
        PriceConfigTrainDto dto = new PriceConfigTrainDto();
        dto.setName(getTrainTypeName());
        dto.setAggregateId(getTrainAggregateId());
        dto.setVersion(getTrainVersion());
        dto.setState(getTrainState() != null ? getTrainState().name() : null);
        return dto;
    }
}