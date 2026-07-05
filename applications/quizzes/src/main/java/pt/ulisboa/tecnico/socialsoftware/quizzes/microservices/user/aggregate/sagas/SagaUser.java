package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.sagas;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.sagas.states.UserSagaState;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Entity
public class SagaUser extends User implements SagaAggregate {
    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    private UserSagaState sagaState;
    
    public SagaUser() {
        super();
        this.sagaState = UserSagaState.NOT_IN_SAGA;
    }
    
    public SagaUser(Integer aggregateId, UserDto userDto) {
        super(aggregateId, userDto);
        this.sagaState = UserSagaState.NOT_IN_SAGA;   
    }
    
    public SagaUser(SagaUser other) {
        super(other);
        this.sagaState = other.getSagaState();
    }
    
    @Override
    public void setSagaState(SagaState state) {
        this.sagaState = (UserSagaState) state;
    }

    @Override
    public UserSagaState getSagaState() {
        return this.sagaState;
    }

    @Override
    public UserSagaState getNeutralSagaState() {
        return UserSagaState.NOT_IN_SAGA;
    }
}
