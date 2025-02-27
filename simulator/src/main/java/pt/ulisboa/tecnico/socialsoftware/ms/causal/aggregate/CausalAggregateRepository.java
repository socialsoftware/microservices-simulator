package pt.ulisboa.tecnico.socialsoftware.ms.causal.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;

import jakarta.transaction.Transactional;

import java.util.Optional;

@Repository
@Transactional
public interface CausalAggregateRepository extends JpaRepository<Aggregate, Integer> {
    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId and a1.state <> 'DELETED' and a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId and a2.version < :unitOfWorkVersion)")
    Optional<Aggregate> findCausal(Integer aggregateId, Integer unitOfWorkVersion);

    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId and a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId and a2.version > :version)")
    Optional<Aggregate> findConcurrentVersions(Integer aggregateId, Integer version);
}
