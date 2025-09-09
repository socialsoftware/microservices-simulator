package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.execution.aggregate.CourseExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

@Service
public class TournamentService {
    private static final Logger logger = LoggerFactory.getLogger(TournamentService.class);

    @Autowired
    private QuizService quizService;
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    private final UnitOfWorkService unitOfWorkService;

    private final TournamentCustomRepository tournamentRepository;

    @Autowired
    private TournamentFactory tournamentFactory;

    public TournamentService(UnitOfWorkService unitOfWorkService, TournamentCustomRepository tournamentRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.tournamentRepository = tournamentRepository;
    }

    // intended for requests from external functionalities
    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
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
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto createTournament(TournamentDto tournamentDto, UserDto creatorDto,
                                          CourseExecutionDto courseExecutionDto, Set<TopicDto> topicDtos,
                                          QuizDto quizDto, UnitOfWork unitOfWork) {

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();

        Tournament tournament = tournamentFactory.createTournament(aggregateId, tournamentDto, creatorDto, courseExecutionDto, topicDtos, quizDto); /* should the skeleton creation be part of the functionality?? */

        unitOfWorkService.registerChanged(tournament, unitOfWork);
        return tournamentFactory.createTournamentDto(tournament);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addParticipant(Integer tournamentAggregateId, TournamentParticipant tournamentParticipant, UnitOfWork unitOfWork) {
        if (tournamentParticipant.getParticipantName().equals("ANONYMOUS") || tournamentParticipant.getParticipantUsername().equals("ANONYMOUS")) {
            throw new QuizzesException(QuizzesErrorMessage.USER_IS_ANONYMOUS, tournamentParticipant.getParticipantAggregateId());
        }
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);

        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        logger.info("Adding participant {} to tournament {}", tournamentParticipant.getId(), tournamentAggregateId);

        newTournament.addParticipant(tournamentParticipant);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class},
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto updateTournament(TournamentDto tournamentDto, Set<TopicDto> topicDtos, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentDto.getAggregateId(), unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        if (tournamentDto.getStartTime() != null ) {
            newTournament.setStartTime(DateHandler.toLocalDateTime(tournamentDto.getStartTime()));
        }

        if (tournamentDto.getEndTime() != null ) {
            newTournament.setEndTime(DateHandler.toLocalDateTime(tournamentDto.getEndTime()));
        }

        if (tournamentDto.getNumberOfQuestions() != null ) {
            newTournament.setNumberOfQuestions(tournamentDto.getNumberOfQuestions());
        }

        if (topicDtos != null && !topicDtos.isEmpty() ) {
            Set<TournamentTopic> tournamentTopics = topicDtos.stream()
                    .map(TournamentTopic::new)
                    .collect(Collectors.toSet());
            newTournament.setTournamentTopics(tournamentTopics);
        }

        unitOfWorkService.registerChanged(newTournament, unitOfWork);

        return tournamentFactory.createTournamentDto(newTournament);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getTournamentsByCourseExecutionId(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return tournamentRepository.findAllRelevantTournamentIds(executionAggregateId).stream()
                .map(aggregateId -> (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(aggregateId, unitOfWork))
                .map(TournamentDto::new)
                .collect(Collectors.toList());

    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        LocalDateTime now = DateHandler.now();
        return tournamentRepository.findAllRelevantTournamentIds(executionAggregateId).stream()
                .map(aggregateId -> (Tournament) unitOfWorkService.aggregateLoad(aggregateId, unitOfWork))
                .filter(t -> now.isBefore(t.getEndTime()))
                .filter(t -> !t.isCancelled())
                .map(tournament -> (Tournament) unitOfWorkService.registerRead(tournament, unitOfWork))
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId, UnitOfWork unitOfWork) {
        LocalDateTime now = DateHandler.now();
        return tournamentRepository.findAllRelevantTournamentIds(executionAggregateId).stream()
                .map(aggregateId -> (Tournament) unitOfWorkService.aggregateLoad(aggregateId, unitOfWork))
                .filter(t -> now.isAfter(t.getEndTime()))
                .filter(t -> !t.isCancelled())
                .map(tournament -> (Tournament) unitOfWorkService.registerRead(tournament, unitOfWork))
                .map(TournamentDto::new)
                .collect(Collectors.toList());
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        TournamentParticipant participantToRemove = newTournament.findParticipant(userAggregateId);
        if (participantToRemove == null) {
            throw new QuizzesException(TOURNAMENT_PARTICIPANT_NOT_FOUND, userAggregateId, tournamentAggregateId);
        }
        newTournament.removeParticipant(participantToRemove);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void solveQuiz(Integer tournamentAggregateId, Integer userAggregateId, Integer answerAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        TournamentParticipant participant = newTournament.findParticipant(userAggregateId);
        if (participant == null) {
            throw new QuizzesException(TOURNAMENT_PARTICIPANT_NOT_FOUND, userAggregateId, tournamentAggregateId);
        }
        participant.answerQuiz();
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.cancel();
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        newTournament.remove();
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }


    /******************************************* EVENT PROCESSING SERVICES ********************************************/

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament anonymizeUser(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId, String name, String username, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        if (!newTournament.getTournamentCourseExecution().getCourseExecutionAggregateId().equals(executionAggregateId)) {
            return null;
        }

        if (newTournament.getTournamentCreator().getCreatorAggregateId().equals(userAggregateId)) {
            newTournament.getTournamentCreator().setCreatorName(name);
            newTournament.getTournamentCreator().setCreatorUsername(username);
            newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
            newTournament.setState(Aggregate.AggregateState.INACTIVE);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }

        for (TournamentParticipant tp : newTournament.getTournamentParticipants()) {
            if (tp.getParticipantAggregateId().equals(userAggregateId)) {
                tp.setParticipantName(name);
                tp.setParticipantUsername(username);
                newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
                unitOfWorkService.registerChanged(newTournament, unitOfWork);
            }
        }
        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament removeCourseExecution(Integer tournamentAggregateId, Integer courseExecutionId, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        if (oldTournament.getTournamentCourseExecution() != null && oldTournament.getTournamentCourseExecution().getCourseExecutionVersion() >= eventVersion) {
            return null;
        }

        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        if (newTournament.getTournamentCourseExecution().getCourseExecutionAggregateId().equals(courseExecutionId)) {
            newTournament.setState(Aggregate.AggregateState.INACTIVE);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }
        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament removeUser(Integer tournamentAggregateId, Integer courseExecutionAggregateId, Integer userAggregateId, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);

        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        if (newTournament.getTournamentCreator().getCreatorAggregateId().equals(userAggregateId)) {
            newTournament.getTournamentCreator().setCreatorState(Aggregate.AggregateState.INACTIVE);
            newTournament.setState(Aggregate.AggregateState.INACTIVE);
            newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }

        TournamentParticipant tournamentParticipant  = newTournament.findParticipant(userAggregateId);
        if (tournamentParticipant != null) {
            tournamentParticipant.setState(Aggregate.AggregateState.DELETED);
            newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
            //tournamentParticipant.setVersion(eventVersion);
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }
        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament updateTopic(Integer tournamentAggregateId, Integer topicAggregateId, String topicName, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        TournamentTopic topic = newTournament.findTopic(topicAggregateId);
        if (topic == null) {
            throw new QuizzesException(TOURNAMENT_TOPIC_NOT_FOUND, topicAggregateId, tournamentAggregateId);
        }
        topic.setTopicName(topicName);
        topic.setTopicVersion(eventVersion);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);

        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament removeTopic(Integer tournamentAggregateId, Integer topicAggregateId, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        TournamentTopic oldTopic = oldTournament.findTopic(topicAggregateId);
        if (oldTopic != null && oldTopic.getTopicVersion() >= eventVersion) {
            return null;
        }
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        TournamentTopic tournamentTopic  = newTournament.findTopic(topicAggregateId);
        if (tournamentTopic == null) {
            throw new QuizzesException(TOURNAMENT_TOPIC_NOT_FOUND, topicAggregateId, tournamentAggregateId);
        }
        newTournament.removeTopic(tournamentTopic);
        QuizDto quizDto = new QuizDto();
        quizDto.setAggregateId(newTournament.getTournamentQuiz().getQuizAggregateId());
        quizDto.setAvailableDate(newTournament.getStartTime().toString());
        quizDto.setConclusionDate(newTournament.getEndTime().toString());
        quizDto.setResultsDate(newTournament.getEndTime().toString());
        try {
            quizService.updateGeneratedQuiz(quizDto, newTournament.getTournamentTopics().stream().filter(t -> t.getState() == Aggregate.AggregateState.ACTIVE).map(TournamentTopic::getTopicAggregateId).collect(Collectors.toSet()), newTournament.getNumberOfQuestions(), unitOfWork);
        } catch (QuizzesException e) {
            newTournament.setState(Aggregate.AggregateState.INACTIVE);
        }

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament updateParticipantAnswer(Integer tournamentAggregateId, Integer studentAggregateId, Integer quizAnswerAggregateId, Integer questionAggregateId, boolean isCorrect, Integer eventVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        TournamentParticipant oldParticipant = oldTournament.findParticipant(studentAggregateId);
        if (oldParticipant != null && oldParticipant.getParticipantAnswer().getQuizAnswerVersion() >= eventVersion) {
            return null;
        }
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        TournamentParticipant tournamentParticipant = newTournament.findParticipant(studentAggregateId);
        if (tournamentParticipant == null) {
            throw new QuizzesException(TOURNAMENT_PARTICIPANT_NOT_FOUND, studentAggregateId, tournamentAggregateId);
        }
        /*
            AFTER_END
                now > this.endTime => p: this.participant | final p.answer
            IS_CANCELED
                this.canceled => final this.startTime && final this.endTime && final this.numberOfQuestions && final this.tournamentTopics && final this.participants && p: this.participant | final p.answer
        */
        if (oldTournament != null) {
            if ((oldTournament.getStartTime() != null && DateHandler.now().isAfter(oldTournament.getStartTime())) || oldTournament.isCancelled()) {
                throw new QuizzesException(CANNOT_UPDATE_TOURNAMENT, oldTournament.getAggregateId());
            }
        }
        tournamentParticipant.updateAnswerWithQuestion(quizAnswerAggregateId, quizAnswerAggregateId, isCorrect, eventVersion);
        unitOfWorkService.registerChanged(newTournament, unitOfWork);
        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Tournament invalidateQuiz(Integer tournamentAggregateId, Integer aggregateId, Integer aggregateVersion, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);
        List<Integer> topicsIds = newTournament.getTournamentTopics().stream().map(TournamentTopic::getTopicAggregateId).collect(Collectors.toList());

        QuizDto quizDto = new QuizDto();
        quizDto.setAvailableDate(newTournament.getStartTime().toString());
        quizDto.setConclusionDate(newTournament.getEndTime().toString());
        quizDto.setResultsDate(newTournament.getEndTime().toString());

        QuizDto quizDto1 = null;
        try {
            quizDto1 = quizService.generateQuiz(newTournament.getTournamentCourseExecution().getCourseExecutionAggregateId(), quizDto, topicsIds, newTournament.getNumberOfQuestions(), unitOfWork);
        } catch (QuizzesException e) {
            newTournament.setState(Aggregate.AggregateState.INACTIVE);
        }

        if (quizDto1 != null) {
            newTournament.getTournamentQuiz().setQuizAggregateId(quizDto1.getAggregateId());
            newTournament.getTournamentQuiz().setQuizVersion(quizDto1.getVersion());
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }

        return newTournament;
    }

    @Retryable(
            value = { SQLException.class,  CannotAcquireLockException.class },
            maxAttemptsExpression = "${retry.db.maxAttempts}",
        backoff = @Backoff(
            delayExpression = "${retry.db.delay}",
            multiplierExpression = "${retry.db.multiplier}"
        ))
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateUserName(Integer tournamentAggregateId, Integer executionAggregateId, Integer eventVersion, Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        boolean changes = false;
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentFromExisting(oldTournament);

        if (!newTournament.getTournamentCourseExecution().getCourseExecutionAggregateId().equals(executionAggregateId)) {
            return;
        }

        if (newTournament.getTournamentCreator().getCreatorAggregateId().equals(userAggregateId)) {
            newTournament.getTournamentCreator().setCreatorName(name);
            newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
            changes = true;
        }

        for (TournamentParticipant tp : newTournament.getTournamentParticipants()) {
            if (tp.getParticipantAggregateId().equals(userAggregateId)) {
                tp.setParticipantName(name);
                newTournament.getTournamentCourseExecution().setCourseExecutionVersion(eventVersion);
                changes = true;
            }
        }
        if (changes) {
            unitOfWorkService.registerChanged(newTournament, unitOfWork);
        }
    }
}
