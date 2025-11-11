package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.Set;

public interface ExecutionCustomRepository {
    Set<Integer> findCourseExecutionIdsOfAllNonDeleted();
}