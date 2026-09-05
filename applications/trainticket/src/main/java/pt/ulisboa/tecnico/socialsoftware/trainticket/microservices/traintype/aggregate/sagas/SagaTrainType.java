package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainTypeDto;

@Entity
public class SagaTrainType extends TrainType implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTrainType() {
    }

    public SagaTrainType(Integer aggregateId, TrainTypeDto trainTypeDto) {
        super(aggregateId, trainTypeDto);
        setSagaState(GenericSagaState.NOT_IN_SAGA);
    }

    public SagaTrainType(SagaTrainType other) {
        super(other);
        setSagaState(other.getSagaState());
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
