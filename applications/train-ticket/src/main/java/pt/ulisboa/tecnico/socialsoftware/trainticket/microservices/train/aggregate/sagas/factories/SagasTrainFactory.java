package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.TrainFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.SagaTrain;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.dtos.SagaTrainDto;

@Service
@Profile("sagas")
public class SagasTrainFactory implements TrainFactory {
    @Override
    public Train createTrain(Integer aggregateId, TrainDto trainDto) {
        return new SagaTrain(aggregateId, trainDto);
    }

    @Override
    public Train createTrainFromExisting(Train existingTrain) {
        return new SagaTrain((SagaTrain) existingTrain);
    }

    @Override
    public TrainDto createTrainDto(Train train) {
        return new SagaTrainDto(train);
    }
}