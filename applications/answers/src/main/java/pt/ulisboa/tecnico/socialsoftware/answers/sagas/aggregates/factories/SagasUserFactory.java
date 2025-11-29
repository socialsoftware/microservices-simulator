package pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.UserDto;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.aggregate.UserFactory;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.SagaUser;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaUserDto;

@Service
@Profile("sagas")
public class SagasUserFactory extends UserFactory {
@Override
public User createUser(Integer aggregateId, UserDto userDto) {
return new SagaUser(userDto);
}

@Override
public User createUserFromExisting(User existingUser) {
return new SagaUser((SagaUser) existingUser);
}

@Override
public UserDto createUserDto(User user) {
return new SagaUserDto(user);
}
}