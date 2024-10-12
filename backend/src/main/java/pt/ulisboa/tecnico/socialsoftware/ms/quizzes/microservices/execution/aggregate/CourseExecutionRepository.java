package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.aggregate;

import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface CourseExecutionRepository extends JpaRepository<CourseExecution, Integer> {
    @Query(value = "select ce1.aggregateId from CourseExecution ce1 where ce1.aggregateId NOT IN (select ce2.aggregateId from CourseExecution ce2 where ce2.state = 'DELETED')")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForTCC();

    @Query(value = "select ce1.aggregateId from CourseExecution ce1 where ce1.aggregateId NOT IN (select ce2.aggregateId from CourseExecution ce2 where ce2.state = 'DELETED' AND ce2.sagaState != 'NOT_IN_SAGA')")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga();
}
