package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas;

import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.User;

@Entity
public class SagaUser extends User implements SagaAggregate {
    @Convert(converter = SagaStateConverter.class)
    private SagaState sagaState;

    public SagaUser() {
    }

    public SagaUser(Integer aggregateId, String name, String username, Role role) {
        super(aggregateId, name, username, role);
        this.sagaState = GenericSagaState.NOT_IN_SAGA;
    }

    public SagaUser(SagaUser other) {
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
