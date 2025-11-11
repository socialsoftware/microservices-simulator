package pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.functionalities;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.causal.unitOfWork.CausalUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesException;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.quiz.aggregate.QuizDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.causal.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.aggregate.UserDto;
import pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.user.service.UserService;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.TCC;
import static pt.ulisboa.tecnico.socialsoftware.quizzes.microservices.exception.QuizzesErrorMessage.*;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService; // testing
    @Autowired
    private UserService userService; // testing
    @Autowired(required = false)
    private SagaUnitOfWorkService sagaUnitOfWorkService;
    @Autowired(required = false)
    private CausalUnitOfWorkService causalUnitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else if (Arrays.asList(activeProfiles).contains(TCC.getValue())) {
            workflowType = TCC;
        } else {
            throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto createTournament(Integer userId, Integer executionId, List<Integer> topicsId, TournamentDto tournamentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userId, topicsId, tournamentDto);

                CreateTournamentFunctionalitySagas createTournamentFunctionalitySagas = new CreateTournamentFunctionalitySagas(
                        sagaUnitOfWorkService,
                        userId, executionId, topicsId, tournamentDto, sagaUnitOfWork, commandGateway);

                createTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTournamentFunctionalitySagas.getTournamentDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(userId, topicsId, tournamentDto);

                CreateTournamentFunctionalityTCC createTournamentFunctionalityTCC = new CreateTournamentFunctionalityTCC(
                        causalUnitOfWorkService,
                        userId, executionId, topicsId, tournamentDto, causalUnitOfWork, commandGateway);

                createTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return createTournamentFunctionalityTCC.getTournamentDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addParticipant(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantFunctionalitySagas addParticipantFunctionalitySagas = new AddParticipantFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentAggregateId, executionAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);

                addParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantFunctionalityTCC addParticipantFunctionalityTCC = new AddParticipantFunctionalityTCC(
                        causalUnitOfWorkService,
                        tournamentAggregateId, userAggregateId, causalUnitOfWork, commandGateway);

                addParticipantFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addParticipantAsync(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantAsyncFunctionalitySagas addParticipantAsyncFunctionalitySagas = new AddParticipantAsyncFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentAggregateId, executionAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);

                addParticipantAsyncFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTournament(TournamentDto tournamentDto, Set<Integer> topicsAggregateIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentFunctionalitySagas updateTournamentFunctionalitySagas = new UpdateTournamentFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentDto, topicsAggregateIds, sagaUnitOfWork, commandGateway);

                updateTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentFunctionalityTCC updateTournamentFunctionalityTCC = new UpdateTournamentFunctionalityTCC(
                        causalUnitOfWorkService,
                        tournamentDto, topicsAggregateIds, causalUnitOfWork, commandGateway);

                updateTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                GetTournamentsForCourseExecutionFunctionalitySagas getTournamentsForCourseExecutionFunctionalitySagas = new GetTournamentsForCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork, commandGateway);

                getTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentsForCourseExecutionFunctionalitySagas.getTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentsForCourseExecutionFunctionalityTCC getTournamentsForCourseExecutionFunctionalityTCC = new GetTournamentsForCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, causalUnitOfWork,
                        commandGateway);

                getTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getTournamentsForCourseExecutionFunctionalityTCC.getTournaments();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getOpenedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOpenedTournamentsForCourseExecutionFunctionalitySagas getOpenedTournamentsForCourseExecutionFunctionalitySagas = new GetOpenedTournamentsForCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork, commandGateway);

                getOpenedTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOpenedTournamentsForCourseExecutionFunctionalitySagas.getOpenedTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOpenedTournamentsForCourseExecutionFunctionalityTCC getOpenedTournamentsForCourseExecutionFunctionalityTCC = new GetOpenedTournamentsForCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, causalUnitOfWork,
                        commandGateway);

                getOpenedTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getOpenedTournamentsForCourseExecutionFunctionalityTCC.getOpenedTournaments();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getClosedTournamentsForCourseExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetClosedTournamentsForCourseExecutionFunctionalitySagas getClosedTournamentsForCourseExecutionFunctionalitySagas = new GetClosedTournamentsForCourseExecutionFunctionalitySagas(
                        sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork, commandGateway);

                getClosedTournamentsForCourseExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getClosedTournamentsForCourseExecutionFunctionalitySagas.getClosedTournaments();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                GetClosedTournamentsForCourseExecutionFunctionalityTCC getClosedTournamentsForCourseExecutionFunctionalityTCC = new GetClosedTournamentsForCourseExecutionFunctionalityTCC(
                        causalUnitOfWorkService, executionAggregateId, causalUnitOfWork,
                        commandGateway);

                getClosedTournamentsForCourseExecutionFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return getClosedTournamentsForCourseExecutionFunctionalityTCC.getClosedTournaments();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void leaveTournament(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                LeaveTournamentFunctionalitySagas leaveTournamentFunctionalitySagas = new LeaveTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId,
                        userAggregateId, sagaUnitOfWork, commandGateway);

                leaveTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                LeaveTournamentFunctionalityTCC leaveTournamentFunctionalityTCC = new LeaveTournamentFunctionalityTCC(
                        causalUnitOfWorkService, tournamentAggregateId,
                        userAggregateId, causalUnitOfWork, commandGateway);

                leaveTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public QuizDto solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SolveQuizFunctionalitySagas solveQuizFunctionalitySagas = new SolveQuizFunctionalitySagas(
                        sagaUnitOfWorkService,
                        tournamentAggregateId, userAggregateId, sagaUnitOfWork, commandGateway);

                solveQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return solveQuizFunctionalitySagas.getQuizDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                SolveQuizFunctionalityTCC solveQuizFunctionalityTCC = new SolveQuizFunctionalityTCC(
                        causalUnitOfWorkService,
                        tournamentAggregateId, userAggregateId, causalUnitOfWork, commandGateway);

                solveQuizFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return solveQuizFunctionalityTCC.getQuizDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void cancelTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CancelTournamentFunctionalitySagas cancelTournamentFunctionalitySagas = new CancelTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId,
                        sagaUnitOfWork, commandGateway);

                cancelTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                CancelTournamentFunctionalityTCC cancelTournamentFunctionalityTCC = new CancelTournamentFunctionalityTCC(
                        causalUnitOfWorkService, tournamentAggregateId,
                        causalUnitOfWork, commandGateway);

                cancelTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                RemoveTournamentFunctionalitySagas removeTournamentFunctionalitySagas = new RemoveTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork,
                        commandGateway);

                removeTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);

                RemoveTournamentFunctionalityTCC removeTournamentFunctionalityTCC = new RemoveTournamentFunctionalityTCC(
                        causalUnitOfWorkService,
                        tournamentAggregateId, causalUnitOfWork, commandGateway);

                removeTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                FindTournamentFunctionalitySagas findTournamentFunctionalitySagas = new FindTournamentFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork,
                        commandGateway);

                findTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findTournamentFunctionalitySagas.getTournamentDto();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);

                FindTournamentFunctionalityTCC findTournamentFunctionalityTCC = new FindTournamentFunctionalityTCC(
                        causalUnitOfWorkService, tournamentAggregateId, causalUnitOfWork,
                        commandGateway);

                findTournamentFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findTournamentFunctionalityTCC.getTournamentDto();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public UserDto findParticipant(Integer tournamentAggregateId, Integer userAggregateId) throws QuizzesException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);

                FindParticipantFunctionalitySagas findParticipantFunctionalitySagas = new FindParticipantFunctionalitySagas(
                        sagaUnitOfWorkService, tournamentAggregateId, userAggregateId,
                        sagaUnitOfWork, commandGateway);

                findParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findParticipantFunctionalitySagas.getParticipant();
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService.createUnitOfWork(functionalityName);
                FindParticipantFunctionalityTCC findParticipantFunctionalityTCC = new FindParticipantFunctionalityTCC(
                        causalUnitOfWorkService, tournamentAggregateId, userAggregateId, causalUnitOfWork,
                        commandGateway);

                findParticipantFunctionalityTCC.executeWorkflow(causalUnitOfWork);
                return findParticipantFunctionalityTCC.getParticipant();
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    /**
     * FOR TESTING PURPOSES
     **/
    public void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId) {
        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService
                        .createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                tournamentService.getTournamentById(tournamentAggregateId, sagaUnitOfWork);
                userService.getUserById(userAggregateId, sagaUnitOfWork);
                break;
            case TCC:
                CausalUnitOfWork causalUnitOfWork = causalUnitOfWorkService
                        .createUnitOfWork(new Throwable().getStackTrace()[0].getMethodName());
                tournamentService.getTournamentById(tournamentAggregateId, causalUnitOfWork);
                userService.getUserById(userAggregateId, causalUnitOfWork);
                break;
            default:
                throw new QuizzesException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(Integer userId, List<Integer> topicsId, TournamentDto tournamentDto) {
        if (userId == null) {
            throw new QuizzesException(TOURNAMENT_MISSING_USER);
        }
        if (topicsId == null) {
            throw new QuizzesException(TOURNAMENT_MISSING_TOPICS);
        }
        if (tournamentDto.getStartTime() == null) {
            throw new QuizzesException(TOURNAMENT_MISSING_START_TIME);
        }
        if (tournamentDto.getEndTime() == null) {
            throw new QuizzesException(TOURNAMENT_MISSING_END_TIME);
        }
        if (tournamentDto.getNumberOfQuestions() == null) {
            throw new QuizzesException(TOURNAMENT_MISSING_NUMBER_OF_QUESTIONS);
        }
    }

}
