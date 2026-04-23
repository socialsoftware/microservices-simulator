package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.courseexecution.aggregate;

import org.springframework.data.jpa.repository.Query;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.Set;

public interface CourseExecutionRepository extends AggregateRepository {

    @Query("select ce1.aggregateId from CourseExecution ce1 where ce1.aggregateId NOT IN (select ce2.aggregateId from CourseExecution ce2 where ce2.state = 'DELETED' AND ce2.sagaState != 'NOT_IN_SAGA')")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga();
}
