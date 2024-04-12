package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToOne;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;

@Entity
public class SagaUser extends User implements SagaAggregate {
    @OneToOne
    private SagaState sagaState;
    
    public SagaUser() {
        super();
    }

    public SagaUser(SagaUser other) {
        super(other);
    }

    public SagaUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
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
