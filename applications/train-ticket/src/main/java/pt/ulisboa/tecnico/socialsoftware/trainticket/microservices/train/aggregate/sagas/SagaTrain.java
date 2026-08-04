package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.sagas;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.train.aggregate.Train;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TrainDto;

@Entity
public class SagaTrain extends Train implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTrain() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTrain(SagaTrain other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTrain(Integer aggregateId, TrainDto trainDto) {
        super(aggregateId, trainDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = state;
    }

    @Override
    public SagaState getSagaState() {
        return this.sagaState;
    }
}