package pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface AggregateRepository extends JpaRepository<Aggregate, Integer> {
    @Query(value = "select a1 from Aggregate a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<Aggregate> findLastAggregateVersion(Integer aggregateId);

}
