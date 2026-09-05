package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import java.util.Set;

public interface ExecutionCustomRepository {
    Set<Integer> findAllExecutionIds();

    Set<Integer> findExecutionIdsByStudent(Integer userAggregateId);
}
