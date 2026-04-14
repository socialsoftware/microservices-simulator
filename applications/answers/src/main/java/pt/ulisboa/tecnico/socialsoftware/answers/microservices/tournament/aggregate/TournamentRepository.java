package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query(value = "select t.aggregateId from Tournament t where t.cancelled = false AND t.aggregateId NOT IN (select t2.aggregateId from Tournament t2 where t2.cancelled = true) AND t.aggregateId NOT IN (select t3.aggregateId from Tournament t3 where t3.state = 'DELETED' AND t3.sagaState != 'NOT_IN_SAGA')")
    Set<Integer> findActiveTournamentIds();
}