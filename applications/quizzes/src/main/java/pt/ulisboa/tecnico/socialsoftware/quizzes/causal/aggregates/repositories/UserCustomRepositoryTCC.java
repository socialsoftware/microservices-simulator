package pt.ulisboa.tecnico.socialsoftware.quizzes.causal.aggregates.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserRepository;


@Service
@Profile("tcc")
public class UserCustomRepositoryTCC implements UserCustomRepository {

    @Autowired
    private UserRepository userRepository;
}
