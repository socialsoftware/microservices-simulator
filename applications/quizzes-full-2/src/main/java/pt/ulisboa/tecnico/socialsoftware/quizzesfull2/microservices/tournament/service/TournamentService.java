package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import pt.ulisboa.tecnico.socialsoftware.ms.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.utils.DateHandler;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.execution.aggregate.ExecutionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2ErrorMessage;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.exception.QuizzesFull2Exception;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.quiz.aggregate.QuizQuestionDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.Tournament;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCreator;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipantQuizAnswer;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopic;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentTopicDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.user.aggregate.UserDto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
public class TournamentService {
    private final TournamentCustomRepository tournamentCustomRepository;
    private final TournamentFactory tournamentFactory;
    private final UnitOfWorkService unitOfWorkService;
    private final AggregateIdGeneratorService aggregateIdGeneratorService;

    public TournamentService(TournamentCustomRepository tournamentCustomRepository,
                             TournamentFactory tournamentFactory,
                             UnitOfWorkService unitOfWorkService,
                             AggregateIdGeneratorService aggregateIdGeneratorService) {
        this.tournamentCustomRepository = tournamentCustomRepository;
        this.tournamentFactory = tournamentFactory;
        this.unitOfWorkService = unitOfWorkService;
        this.aggregateIdGeneratorService = aggregateIdGeneratorService;
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

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public TournamentDto createTournament(ExecutionDto executionDto, UserDto creatorDto,
                                          List<TournamentTopicDto> topics,
                                          List<QuizQuestionDto> selectedQuestions, Integer quizAggregateId,
                                          Long quizVersion, LocalDateTime startTime, LocalDateTime endTime,
                                          Integer numberOfQuestions, UnitOfWork unitOfWork) {
        verifyEnrolled(creatorDto.getAggregateId(), executionDto,
                QuizzesFull2ErrorMessage.CREATOR_COURSE_EXECUTION);
        verifyQuestionsSelected(selectedQuestions, numberOfQuestions);

        Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
        Tournament tournament = tournamentFactory.createTournament(aggregateId,
                executionDto.getAggregateId(), executionDto.getVersion(), executionDto.getCourseAggregateId(),
                creatorDto.getAggregateId(), creatorDto.getName(), creatorDto.getUsername(),
                creatorDto.getVersion(), quizAggregateId, quizVersion, startTime, endTime, numberOfQuestions);

        topics.forEach(topicDto -> tournament.addTopic(toTournamentTopic(topicDto)));

        unitOfWorkService.registerChanged(tournament, unitOfWork);
        return tournamentFactory.createTournamentDto(tournament);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void addParticipant(Integer tournamentAggregateId, UserDto userDto, ExecutionDto executionDto,
                               UnitOfWork unitOfWork) {
        verifyEnrolled(userDto.getAggregateId(), executionDto,
                QuizzesFull2ErrorMessage.PARTICIPANT_COURSE_EXECUTION);

        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        newTournament.addParticipant(new TournamentParticipant(userDto.getAggregateId(), userDto.getName(),
                userDto.getUsername(), userDto.getVersion(), DateHandler.now()));

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void updateTournament(Integer tournamentAggregateId, LocalDateTime startTime, LocalDateTime endTime,
                                 Integer numberOfQuestions, List<TournamentTopicDto> topics,
                                 List<QuizQuestionDto> selectedQuestions, UnitOfWork unitOfWork) {
        verifyQuestionsSelected(selectedQuestions, numberOfQuestions);

        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        newTournament.setStartTime(startTime);
        newTournament.setEndTime(endTime);
        newTournament.setNumberOfQuestions(numberOfQuestions);
        newTournament.setTopics(topics.stream()
                .map(TournamentService::toTournamentTopic)
                .collect(Collectors.toList()));

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void cancelTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        newTournament.setCancelled(true);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void deleteTournament(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        removeTournament(newTournament);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // The creator snapshot and any creator-shaped participant entry carry the same three cached user
    // fields, and TOURNAMENT_CREATOR_PARTICIPANT_CONSISTENCY requires them to agree, so one rename
    // touches every entry for that user.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void setUserName(Integer tournamentAggregateId, Integer userAggregateId, String userName,
                            Long userVersion, UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        TournamentCreator creator = newTournament.getCreator();
        if (Objects.equals(userAggregateId, creator.getUserAggregateId())) {
            creator.setUserName(userName);
            creator.setUserVersion(userVersion);
        }
        newTournament.getParticipants().stream()
                .filter(participant -> Objects.equals(userAggregateId, participant.getUserAggregateId()))
                .forEach(participant -> {
                    participant.setUserName(userName);
                    participant.setUserVersion(userVersion);
                });

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void anonymizeUser(Integer tournamentAggregateId, Integer userAggregateId, String userName,
                              String userUsername, Long userVersion, UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        TournamentCreator creator = newTournament.getCreator();
        if (Objects.equals(userAggregateId, creator.getUserAggregateId())) {
            creator.setUserName(userName);
            creator.setUserUsername(userUsername);
            creator.setUserVersion(userVersion);
        }
        newTournament.getParticipants().stream()
                .filter(participant -> Objects.equals(userAggregateId, participant.getUserAggregateId()))
                .forEach(participant -> {
                    participant.setUserName(userName);
                    participant.setUserUsername(userUsername);
                    participant.setUserVersion(userVersion);
                });

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // A participant is one member of a collection the tournament survives without; the creator is
    // structural - it is constructor-final and CREATOR_IS_NOT_ANONYMOUS reads it - so its deletion
    // takes the whole tournament.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeForDeletedUser(Integer tournamentAggregateId, Integer userAggregateId,
                                     UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        if (Objects.equals(userAggregateId, newTournament.getCreator().getUserAggregateId())) {
            removeTournament(newTournament);
        } else {
            newTournament.removeParticipant(userAggregateId);
        }

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void setTopicName(Integer tournamentAggregateId, Integer topicAggregateId, String topicName,
                             Long topicVersion, UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        newTournament.getTopics().stream()
                .filter(topic -> Objects.equals(topicAggregateId, topic.getTopicAggregateId()))
                .forEach(topic -> {
                    topic.setTopicName(topicName);
                    topic.setTopicVersion(topicVersion);
                });

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // A tournament still draws questions from its remaining topics, so a deleted topic costs it the
    // one entry rather than the whole aggregate.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeDeletedTopic(Integer tournamentAggregateId, Integer topicAggregateId,
                                   UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        newTournament.removeTopic(topicAggregateId);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeForDeletedExecution(Integer tournamentAggregateId, Integer executionAggregateId,
                                          UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        if (!Objects.equals(executionAggregateId,
                newTournament.getExecution().getExecutionAggregateId())) {
            return;
        }
        removeTournament(newTournament);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // Anchored on the execution, so every tournament of that execution is delivered the disenroll
    // event; only the disenrolled student's own participation may be dropped.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeDisenrolledParticipant(Integer tournamentAggregateId, Integer executionAggregateId,
                                             Integer userAggregateId, UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        if (!Objects.equals(executionAggregateId,
                newTournament.getExecution().getExecutionAggregateId())) {
            return;
        }
        newTournament.removeParticipant(userAggregateId);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // An invalidated quiz is treated as deleted downstream, and Tournament.quiz is constructor-final,
    // so the tournament cannot outlive it.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void removeForInvalidatedQuiz(Integer tournamentAggregateId, Integer quizAggregateId,
                                         UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        if (!Objects.equals(quizAggregateId, newTournament.getQuiz().getQuizAggregateId())) {
            return;
        }
        removeTournament(newTournament);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    // Anchored on the tournament's quiz, so the participant is identified by studentAggregateId. The
    // version comparison is what makes the fold idempotent: the same answer event is redelivered under
    // every participant subscription whose cursor still trails it, and the counters must not double.
    @Transactional(isolation = Isolation.SERIALIZABLE)
    public void recordQuestionAnswer(Integer tournamentAggregateId, Integer quizAggregateId,
                                     Integer studentAggregateId, Integer quizAnswerAggregateId,
                                     Boolean correct, LocalDateTime answerTime, Long quizAnswerVersion,
                                     UnitOfWork unitOfWork) {
        Tournament newTournament = loadCopy(tournamentAggregateId, unitOfWork);

        if (!Objects.equals(quizAggregateId, newTournament.getQuiz().getQuizAggregateId())) {
            return;
        }
        TournamentParticipant participant = newTournament.getParticipants().stream()
                .filter(candidate -> Objects.equals(studentAggregateId, candidate.getUserAggregateId()))
                .findFirst()
                .orElse(null);
        if (participant == null) {
            return;
        }
        TournamentParticipantQuizAnswer quizAnswer = participant.getQuizAnswer();
        if (quizAnswer.getQuizAnswerVersion() != null
                && quizAnswer.getQuizAnswerVersion() >= quizAnswerVersion) {
            return;
        }

        quizAnswer.setQuizAnswerAggregateId(quizAnswerAggregateId);
        quizAnswer.setAnswered(true);
        quizAnswer.setNumberOfAnswered(quizAnswer.getNumberOfAnswered() + 1);
        if (Boolean.TRUE.equals(correct)) {
            quizAnswer.setNumberOfCorrect(quizAnswer.getNumberOfCorrect() + 1);
        }
        // Set once: TOURNAMENT_ANSWER_BEFORE_START reads the instant of the student's first answer.
        if (quizAnswer.getFirstAnswerTime() == null) {
            quizAnswer.setFirstAnswerTime(answerTime);
        }
        quizAnswer.setQuizAnswerVersion(quizAnswerVersion);

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
    }

    private Tournament loadCopy(Integer tournamentAggregateId, UnitOfWork unitOfWork) {
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        return tournamentFactory.createTournamentCopy(oldTournament);
    }

    // TOURNAMENT_DELETE fires on the registerChanged that follows, so the participants go first.
    private static void removeTournament(Tournament tournament) {
        tournament.setParticipants(new ArrayList<>());
        tournament.remove();
    }

    private static void verifyEnrolled(Integer userAggregateId, ExecutionDto executionDto, String errorMessage) {
        boolean enrolled = executionDto.getStudents().stream()
                .anyMatch(student -> Objects.equals(student.getUserAggregateId(), userAggregateId));
        if (!enrolled) {
            throw new QuizzesFull2Exception(errorMessage);
        }
    }

    // The saga assembles the selection; only the course's own stock can make it short, and a short
    // quiz would break NUMBER_OF_QUESTIONS / QUIZ_TOPICS the moment the quiz step ran.
    private static void verifyQuestionsSelected(List<QuizQuestionDto> selectedQuestions,
                                                Integer numberOfQuestions) {
        if (selectedQuestions.size() != numberOfQuestions) {
            throw new QuizzesFull2Exception(QuizzesFull2ErrorMessage.TOURNAMENT_NOT_ENOUGH_QUESTIONS);
        }
    }

    private static TournamentTopic toTournamentTopic(TournamentTopicDto topicDto) {
        return new TournamentTopic(topicDto.getTopicAggregateId(), topicDto.getTopicName(),
                topicDto.getTopicVersion(), topicDto.getCourseAggregateId());
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
