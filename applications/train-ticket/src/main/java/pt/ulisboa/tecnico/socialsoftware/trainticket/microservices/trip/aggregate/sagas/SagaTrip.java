package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.sagas;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.trip.aggregate.Trip;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.TripDto;

@Entity
public class SagaTrip extends Trip implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaTrip() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaTrip(SagaTrip other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaTrip(Integer aggregateId, TripDto tripDto) {
        super(aggregateId, tripDto);
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