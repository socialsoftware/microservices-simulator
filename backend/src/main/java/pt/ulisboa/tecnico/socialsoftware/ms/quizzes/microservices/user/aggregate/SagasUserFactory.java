package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.aggregates.SagaUser;

@Service
@Profile("sagas")
public class SagasUserFactory implements UserFactory {

    @Override
    public User createUser(Integer aggregateId, UserDto userDto) {
        return new SagaUser(aggregateId, userDto);
    }

    @Override
    public User createUserFromExisting(User existingUser) {
        return new SagaUser((SagaUser) existingUser);
    }
}