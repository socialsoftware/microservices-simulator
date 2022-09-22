package pt.ulisboa.tecnico.socialsoftware.blcm.tournament.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.service.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.blcm.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.blcm.tournament.domain.*;
import pt.ulisboa.tecnico.socialsoftware.blcm.tournament.dto.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.blcm.tournament.repository.TournamentRepository;
import pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.unityOfWork.UnitOfWork;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate.AggregateState.DELETED;
import static pt.ulisboa.tecnico.socialsoftware.blcm.causalconsistency.aggregate.domain.Aggregate.AggregateState.INACTIVE;
import static pt.ulisboa.tecnico.socialsoftware.blcm.exception.ErrorMessage.*;

@Service
public class TournamentService {
    @Autowired
    private TournamentRepository tournamentRepository;

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    // intended for requests from external functionalities
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TournamentDto getCausalTournamentRemote(Integer aggregateId, UnitOfWork unitOfWorkWorkService) {
        return new TournamentDto(getCausalTournamentLocal(aggregateId, unitOfWorkWorkService));
    }

    // intended for requests from local functionalities

    public Tournament getCausalTournamentLocal(Integer aggregateId, UnitOfWork unitOfWork) {
        Tournament tournament = tournamentRepository.findCausal(aggregateId, unitOfWork.getVersion())
                .orElseThrow(() -> new TutorException(TOURNAMENT_NOT_FOUND, aggregateId));

        if(tournament.getState().equals(DELETED)) {
            throw new TutorException(TOURNAMENT_DELETED, tournament.getAggregateId());
        }

        unitOfWork.addToCausalSnapshot(tournament);
        return tournament;
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TournamentDto> getAllCausalCourseExecutions(UnitOfWork unitOfWork) {
        return tournamentRepository.findAllActive().stream()
                .map(Tournament::getAggregateId)
                .distinct()
                .map(id -> getCausalTournamentLocal(id, unitOfWork))
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TournamentDto createTournament(TournamentDto tournamentDto, TournamentCreator creator,
                                          TournamentCourseExecution courseExecution, Set<TournamentTopic> topics,
                                          TournamentQuiz quiz, UnitOfWork unitOfWorkWorkService) {
        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Tournament tournament = new Tournament(aggregateId, tournamentDto, creator, courseExecution, topics, quiz); /* should the skeleton creation be part of the functionality?? */
        tournament.setPrimaryAggregate(true);
        unitOfWorkWorkService.addUpdatedObject(tournament);
        //unitOfWorkWorkService.addEvent(new TournamentCreationEvent(tournament));
        return new TournamentDto(tournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void addParticipant(Integer tournamentAggregateId, TournamentParticipant tournamentParticipant, UnitOfWork unitOfWorkWorkService) {
        Tournament tournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWorkWorkService);
        Tournament newTournamentVersion = new Tournament(tournament);
        newTournamentVersion.addParticipant(tournamentParticipant);
        unitOfWorkWorkService.addUpdatedObject(newTournamentVersion);
    }

    /*TODO refactor this*/

    /* discuss if the processing for all tournaments should be done at the same time */
    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void anonymizeUser(Integer tournamentAggregateId, Integer userAggregateId, String name, String username, UnitOfWork unitOfWork) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWork);
        Tournament newTournament = new Tournament(oldTournament);

        if(newTournament.getCreator().getAggregateId().equals(userAggregateId)) {
            newTournament.getCreator().setName(name);
            newTournament.getCreator().setUsername(username);
            unitOfWork.addUpdatedObject(newTournament);
        }

        /*TournamentParticipant participantToAnonymize = newTournament.findParticipant(userAggregateId);
        participantToAnonymize.setName(name);
        participantToAnonymize.setUsername(name);*/

        for(TournamentParticipant tp : newTournament.getParticipants()) {
            if(tp.getAggregateId().equals(userAggregateId)) {
                tp.setName(name);
                tp.setUsername(username);
                unitOfWork.addUpdatedObject(newTournament);
            }
        }
    }

    private Set<Tournament> findAllTournamentByVersion(UnitOfWork unitOfWork) {
        Set<Tournament> tournaments = tournamentRepository.findAll()
                .stream()
                .filter(t -> t.getVersion() <= unitOfWork.getVersion())
                .collect(Collectors.toSet());

        Map<Integer, Tournament> tournamentPerAggregateId = new HashMap<>();
        for(Tournament t : tournaments) {
            if(t.getState().equals(DELETED)) {
                throw new TutorException(TOURNAMENT_DELETED, t.getAggregateId());
            }
            unitOfWork.addToCausalSnapshot(t);

            if (!tournamentPerAggregateId.containsKey(t.getAggregateId())) {
                tournamentPerAggregateId.put(t.getAggregateId(), t);
            } else {
                if(tournamentPerAggregateId.get(t.getAggregateId()).getCreationTs().isBefore(t.getCreationTs())) {
                    tournamentPerAggregateId.put(t.getAggregateId(), t);
                }
            }
        }
        return (Set<Tournament>) tournamentPerAggregateId.values();
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TournamentDto updateTournament(TournamentDto tournamentDto, Set<TournamentTopic> tournamentTopics, UnitOfWork unitOfWorkWorkService) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentDto.getAggregateId(), unitOfWorkWorkService);
        Tournament newTournament = new Tournament(oldTournament);

        if(tournamentDto.getStartTime() != null ) {
            newTournament.setStartTime(LocalDateTime.
                    parse(tournamentDto.getStartTime()));
            unitOfWorkWorkService.addUpdatedObject(newTournament);
        }

        if(tournamentDto.getEndTime() != null ) {
            newTournament.setEndTime(LocalDateTime.parse(tournamentDto.getEndTime()));
            unitOfWorkWorkService.addUpdatedObject(newTournament);
        }

        if(tournamentDto.getNumberOfQuestions() != null ) {
            newTournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
            unitOfWorkWorkService.addUpdatedObject(newTournament);
        }

        if(tournamentDto.getTopics() != null ) {
            newTournament.setTopics(tournamentTopics);
            unitOfWorkWorkService.addUpdatedObject(newTournament);
        }

        return new TournamentDto(newTournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWorkWorkService) {
        /*switch this to query???*/
        return findAllTournamentByVersion(unitOfWorkWorkService).stream()
                .filter(t -> t.getCourseExecution().getAggregateId() == executionAggregateId)
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWorkWorkService) {
        /*switch this to query???*/
        LocalDateTime now = LocalDateTime.now();
        return findAllTournamentByVersion(unitOfWorkWorkService).stream()
                .filter(t -> t.getCourseExecution().getAggregateId() == executionAggregateId)
                .filter(t -> now.isBefore(t.getEndTime()))
                .filter(t -> !t.isCancelled())
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWorkWorkService) {
        /*switch this to query???*/
        LocalDateTime now = LocalDateTime.now();
        return findAllTournamentByVersion(unitOfWorkWorkService).stream()
                .filter(t -> t.getCourseExecution().getAggregateId() == executionAggregateId)
                .filter(t -> now.isAfter(t.getEndTime()))
                .filter(t -> !t.isCancelled())
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWorkWorkService) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWorkWorkService);
        Tournament newTournament = new Tournament(oldTournament);
        TournamentParticipant participantToRemove = newTournament.findParticipant(userAggregateId);
        if(participantToRemove == null) {
            throw new TutorException(TOURNAMENT_PARTICIPANT_NOT_FOUND, userAggregateId, tournamentAggregateId);
        }
        newTournament.removeParticipant(participantToRemove);
        unitOfWorkWorkService.addUpdatedObject(newTournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void solveQuiz(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWorkWorkService) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWorkWorkService);
        Tournament newTournament = new Tournament(oldTournament);
        TournamentParticipant participant = newTournament.findParticipant(userAggregateId);
        if(participant == null) {
            throw new TutorException(TOURNAMENT_PARTICIPANT_NOT_FOUND, userAggregateId, tournamentAggregateId);
        }
        participant.answerQuiz();
        unitOfWorkWorkService.addUpdatedObject(newTournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void cancelTournament(Integer tournamentAggregateId, UnitOfWork unitOfWorkWorkService) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWorkWorkService);
        Tournament newTournament = new Tournament(oldTournament);
        newTournament.cancel();
        unitOfWorkWorkService.addUpdatedObject(newTournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWork);
        Tournament newTournament = new Tournament(oldTournament);
        newTournament.remove();
        unitOfWork.addUpdatedObject(newTournament);
    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeCourseExecution(Integer tournamentAggregateId, Integer courseExecutionId, UnitOfWork unitOfWork) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWork);
        Tournament newTournament = new Tournament(oldTournament);
        if(newTournament.getCourseExecution().getAggregateId().equals(courseExecutionId)) {
            newTournament.remove();
            unitOfWork.addUpdatedObject(newTournament);
        }

    }

    @Retryable(
            value = { SQLException.class },
            backoff = @Backoff(delay = 5000))
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void removeUser(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = getCausalTournamentLocal(tournamentAggregateId, unitOfWork);
        Tournament newTournament = new Tournament(oldTournament);
        if(newTournament.getCreator().getAggregateId().equals(userAggregateId)) {
            newTournament.setState(INACTIVE);
            unitOfWork.addUpdatedObject(newTournament);
        }
        TournamentParticipant tournamentParticipant  = newTournament.findParticipant(userAggregateId);
        if(tournamentParticipant == null) {
            throw new TutorException(TOURNAMENT_PARTICIPANT_NOT_FOUND, userAggregateId, tournamentAggregateId);
        }
        tournamentParticipant.setState(DELETED);
        unitOfWork.addUpdatedObject(newTournament);
    }
}
