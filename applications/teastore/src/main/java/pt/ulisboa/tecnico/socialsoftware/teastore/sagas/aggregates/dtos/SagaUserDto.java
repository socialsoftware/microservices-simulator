package pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.dtos;

import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.teastore.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.teastore.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.teastore.sagas.aggregates.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

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