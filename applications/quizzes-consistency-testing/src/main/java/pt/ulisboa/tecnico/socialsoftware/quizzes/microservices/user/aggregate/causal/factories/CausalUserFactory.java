package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.User;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.causal.CausalUser;

@Service
@Profile("tcc")
public class CausalUserFactory implements UserFactory {

    @Override
    public User createUser(Integer aggregateId, UserDto userDto) {
        return new CausalUser(aggregateId, userDto);
    }

    @Override
    public User createUserFromExisting(User existingUser) {
        return new CausalUser((CausalUser) existingUser);
    }

    @Override
    public UserDto createUserDto(User user) {
        return new UserDto(user);
    }
}
