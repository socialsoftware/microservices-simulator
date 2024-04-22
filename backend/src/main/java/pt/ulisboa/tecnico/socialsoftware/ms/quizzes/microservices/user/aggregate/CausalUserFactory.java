package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.aggregates.CausalUser;

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
    
}
