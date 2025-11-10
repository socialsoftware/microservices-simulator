package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate;

import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import jakarta.transaction.Transactional;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query(value = "")
    Set<Integer> findAllRelevantTournamentIds(Integer executionAggregateId);
}