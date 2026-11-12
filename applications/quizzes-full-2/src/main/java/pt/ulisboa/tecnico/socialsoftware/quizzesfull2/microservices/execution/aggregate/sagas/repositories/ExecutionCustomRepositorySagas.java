package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.sagas.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionRepository;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@Profile("sagas")
public class ExecutionCustomRepositorySagas implements ExecutionCustomRepository {
    @Autowired
    private ExecutionRepository executionRepository;

    @Override
    public Set<Integer> findAllExecutionIds() {
        return executionRepository.findAllLatestActive().stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }

    @Override
    public Set<Integer> findExecutionIdsByStudent(Integer userAggregateId) {
        return executionRepository.findAllLatestActiveByStudent(userAggregateId).stream()
                .map(Aggregate::getAggregateId)
                .collect(Collectors.toSet());
    }
}
