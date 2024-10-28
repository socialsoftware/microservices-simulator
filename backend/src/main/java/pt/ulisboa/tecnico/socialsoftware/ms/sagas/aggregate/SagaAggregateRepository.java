package pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

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
