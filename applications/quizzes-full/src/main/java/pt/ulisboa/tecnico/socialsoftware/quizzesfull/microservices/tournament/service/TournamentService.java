package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullException;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.topic.aggregate.TopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentTopic;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;

import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_AFTER_END;
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_IS_CANCELED;
import static pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.exception.QuizzesFullErrorMessage.TOURNAMENT_TOPIC_COURSE_MISMATCH;

@Service
public class TournamentService {

    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private TournamentFactory tournamentFactory;

    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;
    private final TournamentCustomRepository tournamentRepository;

    public TournamentService(UnitOfWorkService unitOfWorkService, TournamentCustomRepository tournamentRepository) {
        this.unitOfWorkService = unitOfWorkService;
        this.tournamentRepository = tournamentRepository;
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto getTournamentById(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        return tournamentFactory.createTournamentDto(
                (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto createTournament(
            Integer executionAggregateId, Long executionVersion, Integer executionCourseId,
            Integer creatorAggregateId, String creatorName, String creatorUsername, Long creatorVersion,
            List<TopicDto> topicDtos,
            Integer quizAggregateId, Long quizVersion,
            LocalDateTime startTime, LocalDateTime endTime, Integer numberOfQuestions,
            UnitOfWork unitOfWork) {

        // [P3] TOPIC_COURSE_EXECUTION: all topics must belong to the execution's course
        for (TopicDto topic : topicDtos) {
            if (!topic.getCourseId().equals(executionCourseId)) {
                throw new QuizzesFullException(TOURNAMENT_TOPIC_COURSE_MISMATCH);
            }
        }

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Set<TournamentTopic> topics = topicDtos.stream()
                .map(TournamentTopic::new)
                .collect(Collectors.toSet());

        Tournament tournament = tournamentFactory.createTournament(
                aggregateId,
                executionAggregateId, executionVersion,
                creatorAggregateId, creatorName, creatorUsername, creatorVersion,
                quizAggregateId, quizVersion,
                topics,
                startTime, endTime, numberOfQuestions);

        unitOfWorkService.registerChanged(tournament, unitOfWork);
        return tournamentFactory.createTournamentDto(tournament);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addParticipant(Integer tournamentAggregateId,
                                Integer userAggregateId, String userName,
                                String userUsername, Long userVersion,
                                UnitOfWork unitOfWork) {

        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        TournamentParticipant participant = new TournamentParticipant(
                userAggregateId, userName, userUsername, userVersion, LocalDateTime.now());
        newTournament.addParticipant(participant);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTournament(Integer tournamentAggregateId,
                                  LocalDateTime startTime, LocalDateTime endTime,
                                  List<TopicDto> topicDtos,
                                  UnitOfWork unitOfWork) {

        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        if (startTime != null) {
            newTournament.setStartTime(startTime);
        }
        if (endTime != null) {
            newTournament.setEndTime(endTime);
        }
        if (topicDtos != null && !topicDtos.isEmpty()) {
            Set<TournamentTopic> topics = topicDtos.stream()
                    .map(TournamentTopic::new)
                    .collect(Collectors.toSet());
            newTournament.setTopics(topics);
        }

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        if (Boolean.TRUE.equals(oldTournament.getCancelled())) {
            throw new QuizzesFullException(TOURNAMENT_IS_CANCELED);
        }
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        newTournament.setCancelled(true);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        // Clear participants before removal (required by TOURNAMENT_DELETE invariant)
        // Note: not allowed for cancelled tournaments with participants (TOURNAMENT_IS_CANCELED fires)
        newTournament.setParticipants(new HashSet<>());
        newTournament.remove();

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public List<TournamentDto> getOpenTournaments(Integer executionAggregateId, UnitOfWork unitOfWork) {
        return tournamentRepository.getOpenTournamentsByExecutionId(executionAggregateId).stream()
                .map(t -> (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(t.getAggregateId(), unitOfWork))
                .map(tournamentFactory::createTournamentDto)
                .collect(Collectors.toList());
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeUserFromTournamentByEvent(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        if (copy.getCreatorAggregateId().equals(userAggregateId)) {
            copy.setParticipants(new HashSet<>());
            copy.remove();
        } else {
            copy.getParticipants().stream()
                    .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
                    .findFirst()
                    .ifPresent(copy::removeParticipant);
        }
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateStudentNameByEvent(Integer tournamentAggregateId, Integer userAggregateId, String name, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        if (copy.getCreatorAggregateId().equals(userAggregateId)) {
            copy.setCreatorName(name);
        }
        copy.getParticipants().stream()
                .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
                .forEach(p -> p.setParticipantName(name));
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeStudentByEvent(Integer tournamentAggregateId, Integer userAggregateId, String name, String username, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        if (copy.getCreatorAggregateId().equals(userAggregateId)) {
            copy.setCreatorName(name);
            copy.setCreatorUsername(username);
            copy.setState(Aggregate.AggregateState.INACTIVE);
        }
        copy.getParticipants().stream()
                .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
                .forEach(p -> {
                    p.setParticipantName(name);
                    p.setParticipantUsername(username);
                });
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTopicNameByEvent(Integer tournamentAggregateId, Integer topicAggregateId, String topicName, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.getTopics().stream()
                .filter(t -> t.getTopicAggregateId().equals(topicAggregateId))
                .forEach(t -> t.setTopicName(topicName));
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeTopicByEvent(Integer tournamentAggregateId, Integer topicAggregateId, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.getTopics().stream()
                .filter(t -> t.getTopicAggregateId().equals(topicAggregateId))
                .findFirst()
                .ifPresent(copy::removeTopic);
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeTournamentByExecutionByEvent(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.setParticipants(new HashSet<>());
        copy.remove();
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeTournamentByQuizByEvent(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.setParticipants(new HashSet<>());
        copy.remove();
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateParticipantAnsweredByEvent(Integer tournamentAggregateId, Integer userAggregateId, UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);
        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.getParticipants().stream()
                .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
                .findFirst()
                .ifPresent(p -> {
                    p.getQuizAnswer().setAnswered(true);
                    p.getQuizAnswer().setNumberOfAnswered(p.getQuizAnswer().getNumberOfAnswered() + 1);
                });
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void setParticipantQuizAnswer(Integer tournamentAggregateId, Integer userAggregateId,
                                          Integer quizAnswerAggregateId, Long quizAnswerVersion,
                                          UnitOfWork unitOfWork) {
        Tournament old = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(tournamentAggregateId, unitOfWork);

        if (Boolean.TRUE.equals(old.getCancelled())) {
            throw new QuizzesFullException(TOURNAMENT_IS_CANCELED);
        }
        if (old.getEndTime() != null && DateHandler.now().isAfter(old.getEndTime())) {
            throw new QuizzesFullException(TOURNAMENT_AFTER_END);
        }

        Tournament copy = tournamentFactory.createTournamentCopy(old);
        copy.getParticipants().stream()
                .filter(p -> p.getParticipantAggregateId().equals(userAggregateId))
                .findFirst()
                .ifPresent(p -> p.getQuizAnswer().linkQuizAnswer(quizAnswerAggregateId, quizAnswerVersion));
        unitOfWorkService.registerChanged(copy, unitOfWork);
    }
}
