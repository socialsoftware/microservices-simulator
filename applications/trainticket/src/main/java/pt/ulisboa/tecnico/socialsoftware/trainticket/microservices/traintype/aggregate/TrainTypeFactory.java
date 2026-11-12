package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

public interface TrainTypeFactory {
    TrainType createTrainType(Integer aggregateId, TrainTypeDto trainTypeDto);

    TrainType createTrainTypeCopy(TrainType existing);

    TrainTypeDto createTrainTypeDto(TrainType trainType);
}
