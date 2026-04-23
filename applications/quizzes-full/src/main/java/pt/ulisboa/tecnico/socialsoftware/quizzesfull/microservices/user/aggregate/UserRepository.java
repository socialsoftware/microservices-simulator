package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.user.aggregate;

import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

public interface UserRepository extends AggregateRepository, UserCustomRepository {
}
