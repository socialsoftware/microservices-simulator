package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate;

import java.util.Set;

public interface CourseExecutionCustomRepository {
    Set<Integer> findCourseExecutionIdsOfAllNonDeleted();
}
