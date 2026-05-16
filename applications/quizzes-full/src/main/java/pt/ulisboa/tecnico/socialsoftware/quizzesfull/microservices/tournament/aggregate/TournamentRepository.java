package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {}
