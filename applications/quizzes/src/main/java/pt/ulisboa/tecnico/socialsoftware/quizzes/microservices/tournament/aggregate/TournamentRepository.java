package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;

@Repository
@Transactional
public interface TournamentRepository extends JpaRepository<Tournament, Integer> {
    @Query(value = "select t1.aggregateId from Tournament t1 where t1.aggregateId NOT IN (select t2.aggregateId from Tournament t2 where t2.state = 'DELETED' OR t2.state = 'INACTIVE') and t1.tournamentCourseExecution.courseExecutionAggregateId = :executionAggregateId")
    Set<Integer> findAllTournamentIdsOfNotDeletedAndNotInactiveByCourseExecutionForTCC(Integer executionAggregateId);

    @Query(value = "select t1.aggregateId from Tournament t1 where t1.aggregateId NOT IN (select t2.aggregateId from Tournament t2 where t2.state = 'DELETED' OR t2.state = 'INACTIVE' OR t2.sagaState != 'NOT_IN_SAGA') and t1.tournamentCourseExecution.courseExecutionAggregateId = :executionAggregateId")
    Set<Integer> findAllTournamentIdsOfNotDeletedAndNotInactiveByCourseExecutionForSaga(Integer executionAggregateId);

    @Query(value = "select t1 from Tournament t1 where t1.version = (select max(t2.version) from Tournament t2)")
    Optional<Tournament> findLatestTournament();
}
