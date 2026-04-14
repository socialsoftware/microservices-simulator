package pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.showcase.microservices.room.aggregate.Room;
import pt.ulisboa.tecnico.socialsoftware.showcase.shared.dtos.RoomDto;

@Entity
public class SagaRoom extends Room implements SagaAggregate {
    private SagaState sagaState;

    public SagaRoom() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaRoom(SagaRoom other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaRoom(Integer aggregateId, RoomDto roomDto) {
        super(aggregateId, roomDto);
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