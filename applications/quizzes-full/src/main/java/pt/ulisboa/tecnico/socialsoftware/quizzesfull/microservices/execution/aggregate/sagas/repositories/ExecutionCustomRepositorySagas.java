package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate.ExecutionRepository;

@Service
@Profile("sagas")
public class ExecutionCustomRepositorySagas implements ExecutionCustomRepository {

    @Autowired
    private ExecutionRepository executionRepository;
}
