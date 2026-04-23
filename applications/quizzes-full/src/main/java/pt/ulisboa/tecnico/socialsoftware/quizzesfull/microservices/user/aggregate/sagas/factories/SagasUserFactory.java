package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.SagaUser;

@Service
@Profile("sagas")
public class SagasUserFactory implements UserFactory {

    @Override
    public User createUser(Integer aggregateId, UserDto userDto) {
        return new SagaUser(aggregateId, userDto);
    }

    @Override
    public User createUserFromExisting(User existing) {
        return new SagaUser((SagaUser) existing);
    }

    @Override
    public UserDto createUserDto(User user) {
        return new UserDto(user);
    }
}
