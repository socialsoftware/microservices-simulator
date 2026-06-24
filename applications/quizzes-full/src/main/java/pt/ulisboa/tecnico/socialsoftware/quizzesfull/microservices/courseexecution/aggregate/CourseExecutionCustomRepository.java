package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

import java.util.Set;

public interface CourseExecutionCustomRepository {
    Set<Integer> findCourseExecutionIdsOfAllNonDeleted();
}
