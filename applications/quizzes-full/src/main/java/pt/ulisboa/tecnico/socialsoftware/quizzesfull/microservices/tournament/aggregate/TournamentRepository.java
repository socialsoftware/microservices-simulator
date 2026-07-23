package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query("select t from Tournament t where t.state = 'ACTIVE' and t.version = (select max(t2.version) from Tournament t2 where t2.aggregateId = t.aggregateId)")
    List<Tournament> findAllLatestActive();
}
