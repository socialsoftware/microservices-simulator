package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.dao.CannotAcquireLockException;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.events.publish.TournamentDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.TournamentExecution;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.enums.TournamentQuiz;

@Service
public class TournamentService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    private final TournamentRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService(UnitOfWorkService unitOfWorkService, TournamentRepository tournamentRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.tournamentRepository = tournamentRepository;
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto createTournament(TournamentDto tournamentDto, UnitOfWork unitOfWork) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Tournament tournament = tournamentFactory.createTournament(aggregateId, tournamentDto);
        unitOfWorkService.registerChanged(tournament, unitOfWork);
        return tournamentFactory.createTournamentDto(tournament);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto getTournamentById(Integer aggregateId, UnitOfWork unitOfWork) {
        return tournamentFactory.createTournamentDto((Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork));
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto updateTournament(Integer aggregateId, TournamentDto tournamentDto, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.setStartTime(tournamentDto.getStartTime());
        newTournament.setEndTime(tournamentDto.getEndTime());
        newTournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
        newTournament.setCancelled(tournamentDto.getCancelled());
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.registerEvent(new TournamentUpdatedEvent(newTournament.getAggregateId(), newTournament.getStartTime(), newTournament.getEndTime(), newTournament.getNumberOfQuestions(), newTournament.getCancelled()), unitOfWork);
        return tournamentFactory.createTournamentDto(newTournament);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTournament(Integer aggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.remove();
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        unitOfWorkService.registerEvent(new TournamentDeletedEvent(newTournament.getAggregateId()), unitOfWork);
    }

    @Retryable(
            value = { SQLException.class, CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
            backoff = @Backoff(
                delayExpression = "${retry.db.delay}",
                multiplierExpression = "${retry.db.multiplier}"
            ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> searchTournaments(Boolean cancelled, TournamentCreator creator, TournamentExecution execution, TournamentQuiz quiz, UnitOfWork unitOfWork) {
        Set<Integer> aggregateIds = tournamentRepository.findAll().stream()
                .filter(entity -> {
                    if (cancelled != null) {
                        if (entity.getCancelled() != cancelled) {
                            return false;
                        }
                    }
                    if (creator != null) {
                        if (!entity.getCreator().equals(creator)) {
                            return false;
                        }
                                            }
                    if (execution != null) {
                        if (!entity.getExecution().equals(execution)) {
                            return false;
                        }
                                            }
                    if (quiz != null) {
                        if (!entity.getQuiz().equals(quiz)) {
                            return false;
                        }
                                            }
                    return true;
                })
                .map(Tournament::getAggregateId)
                .collect(Collectors.toSet());
        return aggregateIds.stream()
                .map(id -> (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork))
                .map(tournamentFactory::createTournamentDto)
                .collect(Collectors.toList());
    }

}
