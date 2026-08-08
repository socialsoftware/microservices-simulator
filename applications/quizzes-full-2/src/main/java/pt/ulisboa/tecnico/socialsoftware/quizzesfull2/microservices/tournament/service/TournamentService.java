package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentFactory;

import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    private final TournamentCustomRepository tournamentCustomRepository;
    private final TournamentFactory tournamentFactory;
    private final UnitOfWorkService unitOfWorkService;

    public TournamentService(TournamentCustomRepository tournamentCustomRepository,
                             TournamentFactory tournamentFactory,
                             UnitOfWorkService unitOfWorkService) {
        this.tournamentCustomRepository = tournamentCustomRepository;
        this.tournamentFactory = tournamentFactory;
        this.unitOfWorkService = unitOfWorkService;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto getTournamentById(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        return tournamentFactory.createTournamentDto(
                (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getTournamentsForExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return findForExecution(executionAggregateId, tournament -> true, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getOpenedTournamentsForExecution(Integer executionAggregateId,
                                                                UnitOfWork unitOfWork) {
        return findForExecution(executionAggregateId, TournamentService::isOpen, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getClosedTournamentsForExecution(Integer executionAggregateId,
                                                                UnitOfWork unitOfWork) {
        return findForExecution(executionAggregateId, TournamentService::isClosed, unitOfWork);
    }

    private List<TournamentDto> findForExecution(Integer executionAggregateId, Predicate<Tournament> selector,
                                                 UnitOfWork unitOfWork) {
        return tournamentCustomRepository.findTournamentIdsByExecution(executionAggregateId).stream()
                .map(tournamentAggregateId -> (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                        tournamentAggregateId, unitOfWork))
                .filter(selector)
                .map(tournamentFactory::createTournamentDto)
                .collect(Collectors.toList());
    }

    // A cancelled tournament is neither open nor closed: it never runs, so it never reaches the end
    // of its window. It stays visible through getTournamentsForExecution.
    private static boolean isOpen(Tournament tournament) {
        return !Boolean.TRUE.equals(tournament.getCancelled())
                && tournament.getEndTime().isAfter(DateHandler.now());
    }

    private static boolean isClosed(Tournament tournament) {
        return !Boolean.TRUE.equals(tournament.getCancelled())
                && !tournament.getEndTime().isAfter(DateHandler.now());
    }
}
