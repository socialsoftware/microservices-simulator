package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.station.aggregate.Station;

@Entity
public class SagaStation extends Station implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaStation() {
    }

    public SagaStation(Integer aggregateId, String name, Integer stayTime) {
        super(aggregateId, name, stayTime);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaStation(SagaStation other) {
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
