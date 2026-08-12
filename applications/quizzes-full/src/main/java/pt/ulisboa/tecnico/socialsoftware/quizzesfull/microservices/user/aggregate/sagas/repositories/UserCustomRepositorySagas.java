package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate.UserRepository;

@Service
@Profile("sagas")
public class UserCustomRepositorySagas implements UserCustomRepository {

    @Autowired
    private UserRepository userRepository;
}
