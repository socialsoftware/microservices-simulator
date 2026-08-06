package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas.SagaTrain;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate.SagaState;

public class SagaTrainDto extends TrainDto {
@Convert(converter = SagaStateConverter.class)
private SagaState sagaState;

public SagaTrainDto(Train train) {
super((Train) train);
this.sagaState = ((SagaTrain)train).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}