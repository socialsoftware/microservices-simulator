package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.Role;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class UserCustomRepositorySagas implements UserCustomRepository {
    @Autowired
    private UserRepository userRepository;

    @Override
    public Set<Integer> findUserIdsByRole(Role role) {
        return userRepository.findAllLatestActiveByRole(role).stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}
