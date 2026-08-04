package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.dtos;

import jakarta.persistence.Convert;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaStateConverter;

import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.trainticket.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.user.aggregate.sagas.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;

public class SagaUserDto extends UserDto {
@Convert(converter = SagaStateConverter.class)
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