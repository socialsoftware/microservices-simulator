package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaState;

@Entity
public class SagaUser extends User implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    private SagaState sagaState;
    
    public SagaUser() {
        super();
        this.sagaState = SagaState.NOT_IN_SAGA;
    }
    
    public SagaUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
        this.sagaState = SagaState.NOT_IN_SAGA;   
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
