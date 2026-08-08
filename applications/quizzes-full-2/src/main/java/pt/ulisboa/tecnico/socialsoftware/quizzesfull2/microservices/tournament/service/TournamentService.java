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
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentCustomRepository;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentParticipant;
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
        Tournament oldTournament = (Tournament) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        Tournament newTournament = tournamentFactory.createTournamentCopy(oldTournament);

        // TOURNAMENT_DELETE fires inside remove()'s verifyInvariants, so the participants go first.
        newTournament.setParticipants(new ArrayList<>());
        newTournament.remove();

        unitOfWorkService.registerChanged(newTournament, unitOfWork);
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
