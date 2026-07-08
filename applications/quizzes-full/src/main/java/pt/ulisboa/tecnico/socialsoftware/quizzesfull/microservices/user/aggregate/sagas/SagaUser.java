package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas;

import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

@Entity
public class SagaUser extends User implements SagaAggregate {

    @Convert(converter = SagaStateConverter.class)

    private SagaState sagaState;

    public SagaUser() {
        super();
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaUser(SagaUser other) {
        super(other);
        this.sagaState = other.getSagaState();
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
