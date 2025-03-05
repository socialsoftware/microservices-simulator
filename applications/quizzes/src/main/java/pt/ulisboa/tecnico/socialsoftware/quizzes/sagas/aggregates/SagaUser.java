package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate;

@Entity
public class SagaUser extends User implements SagaAggregate {
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
