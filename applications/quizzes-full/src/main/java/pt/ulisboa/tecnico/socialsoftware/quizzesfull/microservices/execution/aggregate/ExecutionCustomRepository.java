package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.execution.aggregate;

import java.util.Set;

public interface ExecutionCustomRepository {
    Set<Integer> findExecutionIdsOfAllNonDeleted();
}
