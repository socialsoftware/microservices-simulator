package pt.ulisboa.tecnico.socialsoftware.answers.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.exception.AnswersException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.user.service.UserService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private TournamentService tournamentService;

    @Autowired
    private UserService userService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto createTournament(Integer executionId, Integer userId, String topicsId, TournamentDto tournamentDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                CreateTournamentFunctionalitySagas createTournamentFunctionalitySagas = new CreateTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionId, userId, topicsId, tournamentDto, sagaUnitOfWork);
                createTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createTournamentFunctionalitySagas.getCreatedTournament();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void addParticipant(Integer tournamentAggregateId, Integer executionAggregateId, Integer userAggregateId) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                AddParticipantFunctionalitySagas addParticipantFunctionalitySagas = new AddParticipantFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregateId, executionAggregateId, userAggregateId, sagaUnitOfWork);
                addParticipantFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void updateTournament(String topicsId, TournamentDto tournamentDto) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                UpdateTournamentFunctionalitySagas updateTournamentFunctionalitySagas = new UpdateTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, topicsId, tournamentDto, sagaUnitOfWork);
                updateTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public TournamentDto findTournament(Integer tournamentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                FindTournamentFunctionalitySagas findTournamentFunctionalitySagas = new FindTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregateId, sagaUnitOfWork);
                findTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return findTournamentFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public String solveQuiz(Integer tournamentAggregateId, Integer userAggregateId) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                SolveQuizFunctionalitySagas solveQuizFunctionalitySagas = new SolveQuizFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregateId, userAggregateId, sagaUnitOfWork);
                solveQuizFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return solveQuizFunctionalitySagas.getResult();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getTournamentsForExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentsForExecutionFunctionalitySagas getTournamentsForExecutionFunctionalitySagas = new GetTournamentsForExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                getTournamentsForExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getTournamentsForExecutionFunctionalitySagas.getTournamentsForExecution();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getOpenedTournamentsForExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetOpenedTournamentsForExecutionFunctionalitySagas getOpenedTournamentsForExecutionFunctionalitySagas = new GetOpenedTournamentsForExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                getOpenedTournamentsForExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getOpenedTournamentsForExecutionFunctionalitySagas.getOpenedTournamentsForExecution();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<TournamentDto> getClosedTournamentsForExecution(Integer executionAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetClosedTournamentsForExecutionFunctionalitySagas getClosedTournamentsForExecutionFunctionalitySagas = new GetClosedTournamentsForExecutionFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, executionAggregateId, sagaUnitOfWork);
                getClosedTournamentsForExecutionFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getClosedTournamentsForExecutionFunctionalitySagas.getClosedTournamentsForExecution();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void removeTournament(Integer tournamentAggregate) throws AnswersException {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                RemoveTournamentFunctionalitySagas removeTournamentFunctionalitySagas = new RemoveTournamentFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregate, sagaUnitOfWork);
                removeTournamentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void getTournamentAndUser(Integer tournamentAggregateId, Integer userAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetTournamentAndUserFunctionalitySagas getTournamentAndUserFunctionalitySagas = new GetTournamentAndUserFunctionalitySagas(
                        tournamentService, sagaUnitOfWorkService, tournamentAggregateId, userAggregateId, sagaUnitOfWork);
                getTournamentAndUserFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

}