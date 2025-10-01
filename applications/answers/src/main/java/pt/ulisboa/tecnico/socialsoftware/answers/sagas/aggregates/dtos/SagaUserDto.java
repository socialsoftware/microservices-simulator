package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaUser;
import ${this.getBasePackage()}.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaUserDto extends UserDto {
private SagaState sagaState;

public SagaUserDto(User user) {
super((User) user);
this.sagaState = ((SagaUser)user).getSagaState();
}

public SagaState getSagaState() {
return this.sagaState;
}

public void setSagaState(SagaState sagaState) {
this.sagaState = sagaState;
}
}