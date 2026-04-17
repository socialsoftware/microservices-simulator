package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.causal.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserFactory;

@Service
@Profile("tcc")
public class CausalUserFactory implements UserFactory {

    @Override
    public User createUser(Integer aggregateId, UserDto userDto) {
        throw new UnsupportedOperationException("TCC not implemented");
    }

    @Override
    public User createUserFromExisting(User existing) {
        throw new UnsupportedOperationException("TCC not implemented");
    }

    @Override
    public UserDto createUserDto(User user) {
        throw new UnsupportedOperationException("TCC not implemented");
    }
}
