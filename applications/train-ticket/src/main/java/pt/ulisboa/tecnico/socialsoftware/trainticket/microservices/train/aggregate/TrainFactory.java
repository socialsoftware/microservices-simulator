package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate;

import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

public interface TrainFactory {
    Train createTrain(Integer aggregateId, TrainDto trainDto);
    Train createTrainFromExisting(Train existingTrain);
    TrainDto createTrainDto(Train train);
}
