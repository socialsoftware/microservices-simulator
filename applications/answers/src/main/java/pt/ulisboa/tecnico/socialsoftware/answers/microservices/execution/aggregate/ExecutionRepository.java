package pt.ulisboa.tecnico.socialsoftware.answers.microservices.execution.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface ExecutionRepository extends JpaRepository<Execution, Integer> {
    @Query(value = "select e1.aggregateId from Execution e1 where e1.aggregateId NOT IN (select e2.aggregateId from Execution e2 where e2.state = 'DELETED' AND e2.sagaState != 'NOT_IN_SAGA') AND e1.sagaState = 'NOT_IN_SAGA'")
    Set<Integer> findCourseExecutionIdsOfAllNonDeletedForSaga();
}