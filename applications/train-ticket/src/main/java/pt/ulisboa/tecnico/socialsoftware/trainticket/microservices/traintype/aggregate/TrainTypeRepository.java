package pt.ulisboa.tecnico.socialsoftware.trainticket.microservices.traintype.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface TrainTypeRepository extends JpaRepository<TrainType, Integer> {
    @Query("select t from TrainType t " +
            "where t.state = 'ACTIVE' " +
            "and t.name = :name " +
            "and t.version = (select max(t2.version) from TrainType t2 where t2.aggregateId = t.aggregateId)")
    List<TrainType> findAllLatestActiveByName(@Param("name") String name);

    @Query(value = "select a1 from TrainType a1 where a1.aggregateId = :aggregateId AND a1.state = 'ACTIVE' AND a1.version = (select max(a2.version) from Aggregate a2 where a2.aggregateId = :aggregateId)")
    Optional<TrainType> findLastAggregateVersion(Integer aggregateId);

    Optional<TrainType> findTopByOrderByVersionDesc();
}
