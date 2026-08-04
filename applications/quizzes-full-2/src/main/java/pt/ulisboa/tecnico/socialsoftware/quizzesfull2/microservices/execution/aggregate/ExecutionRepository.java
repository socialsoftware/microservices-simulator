package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateRepository;

import java.util.List;

@Repository
public interface ExecutionRepository extends AggregateRepository {
    @Query("select e from Execution e " +
            "where e.state = 'ACTIVE' " +
            "and e.version = (select max(e2.version) from Execution e2 where e2.aggregateId = e.aggregateId)")
    List<Execution> findAllLatestActive();

    @Query("select e from Execution e join e.students s " +
            "where e.state = 'ACTIVE' " +
            "and s.userAggregateId = :userAggregateId " +
            "and e.version = (select max(e2.version) from Execution e2 where e2.aggregateId = e.aggregateId)")
    List<Execution> findAllLatestActiveByStudent(@Param("userAggregateId") Integer userAggregateId);
}
