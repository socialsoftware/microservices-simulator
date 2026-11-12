package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeFactory;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas.SagaTrainType;

@Service
@Profile("sagas")
public class SagasTrainTypeFactory implements TrainTypeFactory {
    @Override
    public SagaTrainType createTrainType(Integer aggregateId, TrainTypeDto trainTypeDto) {
        return new SagaTrainType(aggregateId, trainTypeDto);
    }

    @Override
    public SagaTrainType createTrainTypeCopy(TrainType existing) {
        return new SagaTrainType((SagaTrainType) existing);
    }

    @Override
    public TrainTypeDto createTrainTypeDto(TrainType trainType) {
        return new TrainTypeDto(trainType);
    }
}
