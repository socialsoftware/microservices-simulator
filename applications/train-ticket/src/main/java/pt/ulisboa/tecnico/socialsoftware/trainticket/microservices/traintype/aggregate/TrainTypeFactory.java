package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

public interface TrainTypeFactory {
    TrainType createTrainType(Integer aggregateId, String name, Integer economyClassSeats, Integer firstClassSeats,
                              Integer averageSpeed);

    TrainType createTrainTypeCopy(TrainType existing);

    TrainTypeDto createTrainTypeDto(TrainType trainType);
}
