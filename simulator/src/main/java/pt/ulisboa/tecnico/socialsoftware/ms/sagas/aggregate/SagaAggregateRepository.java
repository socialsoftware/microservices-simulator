package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import java.util.Optional;

@Repository
@Transactional
public interface SagaAggregateRepository extends JpaRepository<Aggregate, Integer> {
    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId and a1.state <> 'DELETED' and a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Aggregate> findNonDeletedSagaAggregate(Integer aggregateId);

    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId and a1.state = 'DELETED' and a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Aggregate> findDeletedSagaAggregate(Integer aggregateId);

    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId and a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Aggregate> findAnySagaAggregate(Integer aggregateId);
}
