package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;

@Entity
public class SagaUser extends User implements SagaAggregate {
    private SagaState sagaState;

    public SagaUser() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaUser(SagaUser other) {
        super(other);
        this.sagaState = other.getSagaState();
    }

    public SagaUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
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