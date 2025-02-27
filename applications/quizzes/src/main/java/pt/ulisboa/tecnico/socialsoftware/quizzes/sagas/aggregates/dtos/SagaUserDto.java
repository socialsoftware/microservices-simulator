package pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.sagas.aggregates.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaUserDto extends UserDto {
    private SagaState sagaState;

    public SagaUserDto(User user) {
        super(user);
        this.sagaState = ((SagaUser)user).getSagaState();
    }

    public SagaState getSagaState() {
        return this.sagaState;
    }

    public void setSagaState(SagaState sagaState) {
        this.sagaState = sagaState;
    }
}
