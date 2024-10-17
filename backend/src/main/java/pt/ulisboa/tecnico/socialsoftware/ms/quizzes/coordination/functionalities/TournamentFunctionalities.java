package pt.ulisboa.tecnico.socialsoftware.ms.quizzes.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.ErrorMessage.*;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.MicroservicesSimulator;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unityOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.causal.coordination.*;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.answer.service.QuizAnswerService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.exception.TutorException;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.execution.service.CourseExecutionService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.aggregate.QuizFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.quiz.service.QuizService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.topic.service.TopicService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.aggregate.TournamentFactory;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.events.handling.TournamentEventHandling;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.ms.quizzes.sagas.coordination.workflows.*;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unityOfWork.SagaUnitOfWorkService;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;
    @Autowired
    private UserService userService;
    @Autowired
    private CourseExecutionService courseExecutionService;
    @Autowired
    private TopicService topicService;
    @Autowired
    private QuizService quizService;
    @Autowired
    private QuizAnswerService quizAnswerService;
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private TournamentFactory tournamentFactory;
    @Autowired
    private QuizFactory quizFactory;
    @Autowired
    private TournamentEventHandling tournamentEventHandling;
    @Autowired
    private EventService eventService;

    @Autowired
    private Environment env;

    private MicroservicesSimulator.TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userId, topicsId, tournamentDto);

                CreateTournamentFunctionalitySagas createTournamentFunctionalitySagas = new CreateTournamentFunctionalitySagas(
                        tournamentService, courseExecutionService, topicService, quizService, sagaUnitOfWorkService,
                        userId, executionId, topicsId, tournamentDto, sagaUnitOfWork);

                createTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTournamentFunctionalitySagas.getTournamentDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userId, topicsId, tournamentDto);

                CreateTournamentFunctionalityTCC createTournamentFunctionalityTCC = new CreateTournamentFunctionalityTCC(
                        tournamentService, courseExecutionService, topicService, quizService, causalUnitOfWorkService,
                        userId, executionId, topicsId, tournamentDto, causalUnitOfWork);

                createTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createTournamentFunctionalityTCC.getTournamentDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addParticipant(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantFunctionalitySagas addParticipantFunctionalitySagas = new AddParticipantFunctionalitySagas(
                        eventService, tournamentEventHandling, tournamentService, courseExecutionService, sagaUnitOfWorkService,
                        tournamentAggregateId, executionAggregateId, userAggregateId, sagaUnitOfWork);

                addParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantFunctionalityTCC addParticipantFunctionalityTCC = new AddParticipantFunctionalityTCC(
                        eventService, tournamentEventHandling, tournamentService, courseExecutionService, causalUnitOfWorkService,
                        tournamentAggregateId, userAggregateId, causalUnitOfWork);

                addParticipantFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentFunctionalitySagas updateTournamentFunctionalitySagas = new UpdateTournamentFunctionalitySagas(
                        tournamentService, topicService, quizService, sagaUnitOfWorkService, tournamentFactory, quizFactory,
                        tournamentDto, topicsAggregateIds, sagaUnitOfWork);

                updateTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentFunctionalityTCC updateTournamentFunctionalityTCC = new UpdateTournamentFunctionalityTCC(
                        tournamentService, topicService, quizService, causalUnitOfWorkService, tournamentFactory, quizFactory,
                        tournamentDto, topicsAggregateIds, causalUnitOfWork);

                updateTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                GetTournamentsForCourseExecutionFunctionalitySagas getTournamentsForCourseExecutionFunctionalitySagas = new GetTournamentsForCourseExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);

                getTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentsForCourseExecutionFunctionalitySagas.getTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentsForCourseExecutionFunctionalityTCC getTournamentsForCourseExecutionFunctionalityTCC = new GetTournamentsForCourseExecutionFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, executionAggregateId, causalUnitOfWork);

                getTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getTournamentsForCourseExecutionFunctionalityTCC.getTournaments();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOpenedTournamentsForCourseExecutionFunctionalitySagas getOpenedTournamentsForCourseExecutionFunctionalitySagas = new GetOpenedTournamentsForCourseExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);

                getOpenedTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOpenedTournamentsForCourseExecutionFunctionalitySagas.getOpenedTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOpenedTournamentsForCourseExecutionFunctionalityTCC getOpenedTournamentsForCourseExecutionFunctionalityTCC = new GetOpenedTournamentsForCourseExecutionFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, executionAggregateId, causalUnitOfWork);

                getOpenedTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getOpenedTournamentsForCourseExecutionFunctionalityTCC.getOpenedTournaments();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetClosedTournamentsForCourseExecutionFunctionalitySagas getClosedTournamentsForCourseExecutionFunctionalitySagas = new GetClosedTournamentsForCourseExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);

                getClosedTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getClosedTournamentsForCourseExecutionFunctionalitySagas.getClosedTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetClosedTournamentsForCourseExecutionFunctionalityTCC getClosedTournamentsForCourseExecutionFunctionalityTCC = new GetClosedTournamentsForCourseExecutionFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, executionAggregateId, causalUnitOfWork);

                getClosedTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getClosedTournamentsForCourseExecutionFunctionalityTCC.getClosedTournaments();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                LeaveTournamentFunctionalitySagas leaveTournamentFunctionalitySagas = new LeaveTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, sagaUnitOfWork);

                leaveTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                LeaveTournamentFunctionalityTCC leaveTournamentFunctionalityTCC = new LeaveTournamentFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, userAggregateId, causalUnitOfWork);

                leaveTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SolveQuizFunctionalitySagas solveQuizFunctionalitySagas = new SolveQuizFunctionalitySagas(
                        tournamentService, quizService, quizAnswerService, sagaUnitOfWorkService, tournamentFactory,
                        tournamentAggregateId, userAggregateId, sagaUnitOfWork);

                solveQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return solveQuizFunctionalitySagas.getQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                SolveQuizFunctionalityTCC solveQuizFunctionalityTCC = new SolveQuizFunctionalityTCC(
                        tournamentService, quizService, quizAnswerService, causalUnitOfWorkService, tournamentFactory,
                        tournamentAggregateId, userAggregateId, causalUnitOfWork);

                solveQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return solveQuizFunctionalityTCC.getQuizDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void cancelTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CancelTournamentFunctionalitySagas cancelTournamentFunctionalitySagas = new CancelTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, sagaUnitOfWork);

                cancelTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CancelTournamentFunctionalityTCC cancelTournamentFunctionalityTCC = new CancelTournamentFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, causalUnitOfWork);

                cancelTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                RemoveTournamentFunctionalitySagas removeTournamentFunctionalitySagas = new RemoveTournamentFunctionalitySagas(
                        eventService, tournamentService, sagaUnitOfWorkService, tournamentFactory, tournamentAggregateId, sagaUnitOfWork);

                removeTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);

                RemoveTournamentFunctionalityTCC removeTournamentFunctionalityTCC = new RemoveTournamentFunctionalityTCC(
                        eventService, tournamentService, causalUnitOfWorkService, tournamentFactory, tournamentAggregateId, causalUnitOfWork);

                removeTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                FindTournamentFunctionalitySagas findTournamentFunctionalitySagas = new FindTournamentFunctionalitySagas(tournamentService, sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork);

                findTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findTournamentFunctionalitySagas.getTournamentDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);

                FindTournamentFunctionalityTCC findTournamentFunctionalityTCC = new FindTournamentFunctionalityTCC(
                        tournamentService, causalUnitOfWorkService, tournamentAggregateId, causalUnitOfWork);

                findTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findTournamentFunctionalityTCC.getTournamentDto();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto findParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws TutorException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                FindParticipantFunctionalitySagas findParticipantFunctionalitySagas = new FindParticipantFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregateId, userAggregateId, sagaUnitOfWork);

                findParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findParticipantFunctionalitySagas.getParticipant();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindParticipantFunctionalityTCC findParticipantFunctionalityTCC = new FindParticipantFunctionalityTCC(
                        causalUnitOfWorkService, tournamentAggregateId, userAggregateId, causalUnitOfWork);

                findParticipantFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findParticipantFunctionalityTCC.getParticipant();
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    /** FOR TESTING PURPOSES **/
    public void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId) {
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                TournamentDto tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, sagaUnitOfWork);
                UserDto userDto = userService.getUserById(userAggregateId, sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                tournamentDto = tournamentService.getTournamentById(tournamentAggregateId, causalUnitOfWork);
                userDto = userService.getUserById(userAggregateId, causalUnitOfWork);
                break;
            default: throw new TutorException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(Integer userId, List<Integer> topicsId, TournamentDto tournamentDto) {
        if (userId == null) {
            throw new TutorException(TOURNAMENT_MISSING_USER);
        }
        if (topicsId == null) {
            throw new TutorException(TOURNAMENT_MISSING_TOPICS);
        }
        if (tournamentDto.getStartTime() == null) {
            throw new TutorException(TOURNAMENT_MISSING_START_TIME);
        }
        if (tournamentDto.getEndTime() == null) {
            throw new TutorException(TOURNAMENT_MISSING_END_TIME);
        }
        if (tournamentDto.getNumberOfQuestions() == null) {
            throw new TutorException(TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS);
        }
    }

}
