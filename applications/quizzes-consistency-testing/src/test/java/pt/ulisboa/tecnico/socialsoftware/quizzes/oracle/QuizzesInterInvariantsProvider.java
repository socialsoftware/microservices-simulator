package pt.ulisboa.tecnico.socialsoftware.quizzes.oracle;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariant;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantViolation;
import pt.ulisboa.tecnico.socialsoftware.consistencytesting.oracle.InterInvariantsProvider;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.AddParticipantWithinMaxTournamentsFunctionalitySagas.MAX_TOURNAMENTS_PER_USER;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

@Component
@Profile("oracle")
public class QuizzesInterInvariantsProvider implements InterInvariantsProvider {

    private static final String MAX_TOURNAMENTS_PER_USER_INVARIANT = "user participates in at most "
            + MAX_TOURNAMENTS_PER_USER + " tournament(s) per course execution";

    /** A user within one course execution — the scope the quota is counted over. */
    private record ExecutionUser(Integer executionAggregateId, Integer userAggregateId) {
    }

    @Autowired
    private TournamentRepository tournamentRepository;
    @Autowired
    private TournamentService tournamentService;
    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Override
    public @NonNull Set<InterInvariant> getInterInvariants() {
        return Set.of(new InterInvariant(
                MAX_TOURNAMENTS_PER_USER_INVARIANT,
                this::findMaxTournamentsPerUserViolations));
    }

    /**
     * Scans the current tournaments, counts how many of them each user participates
     * in within each course execution, and reports every (execution, user) pair
     * that exceeds
     * {@link AddParticipantWithinMaxTournamentsFunctionalitySagas#MAX_TOURNAMENTS_PER_USER}.
     * <p>
     * This is a cross-tournament predicate the intra-aggregate consistency layer
     * cannot enforce: it only becomes observable after two concurrent joins have
     * committed.
     */
    private Set<InterInvariantViolation> findMaxTournamentsPerUserViolations() {
        SagaUnitOfWork unitOfWork = sagaUnitOfWorkService.createUnitOfWork(
                "interInvariantMaxTournamentsPerUser");

        // The repository holds every VERSION of every tournament, so it is queried for
        // the distinct aggregate ids and each one is then resolved through the service,
        // which returns the single version visible to this unit of work.
        Map<ExecutionUser, Integer> tournamentCount = new HashMap<>();
        for (Integer tournamentId : tournamentRepository.findAllAggregateIds()) {
            TournamentDto tournament;
            try {
                tournament = tournamentService.getTournamentById(tournamentId, unitOfWork);
            } catch (RuntimeException e) {
                // Not an active/loadable tournament (e.g. deleted); it holds no participants.
                continue;
            }
            if (tournament == null ||
                    tournament.getParticipants() == null ||
                    tournament.getCourseExecution() == null) {
                continue;
            }
            Integer executionId = tournament.getCourseExecution().getAggregateId();
            for (UserDto participant : tournament.getParticipants()) {
                tournamentCount.merge(
                        new ExecutionUser(executionId, participant.getAggregateId()), 1, Integer::sum);
            }
        }

        return tournamentCount.entrySet().stream()
                .filter(entry -> entry.getValue() > MAX_TOURNAMENTS_PER_USER)
                .map(entry -> new InterInvariantViolation(
                        "user %d participates in %d tournaments of course execution %d (max %d)"
                                .formatted(entry.getKey().userAggregateId(), entry.getValue(),
                                        entry.getKey().executionAggregateId(), MAX_TOURNAMENTS_PER_USER)))
                .collect(Collectors.toSet());
    }
}
