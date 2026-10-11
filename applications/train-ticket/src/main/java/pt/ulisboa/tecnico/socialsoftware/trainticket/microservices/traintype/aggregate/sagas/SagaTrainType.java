package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate.TrainType;

@Entity
public class SagaTrainType extends TrainType implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTrainType() {
    }

    public SagaTrainType(Integer aggregateId, String name, Integer economyClassSeats, Integer firstClassSeats,
                         Integer averageSpeed) {
        super(aggregateId, name, economyClassSeats, firstClassSeats, averageSpeed);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTrainType(SagaTrainType other) {
        super(other);
        this.sagaState = other.getSagaState();
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
