package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.GenericSagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.aggregate.SagaAggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.CancelTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.DeleteTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetClosedTournamentsForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetOpenedTournamentsForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetTournamentByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetTournamentsForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.service.TournamentService;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;
    @Autowired
    private TournamentService tournamentService;

    public TournamentDto getTournamentById(Integer tournamentAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTournamentById");
        GetTournamentByIdFunctionalitySagas saga = new GetTournamentByIdFunctionalitySagas(
                unitOfWorkService, tournamentAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournamentDto();
    }

    public List<TournamentDto> getTournamentsForExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getTournamentsForExecution");
        GetTournamentsForExecutionFunctionalitySagas saga = new GetTournamentsForExecutionFunctionalitySagas(
                unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournaments();
    }

    public List<TournamentDto> getOpenedTournamentsForExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getOpenedTournamentsForExecution");
        GetOpenedTournamentsForExecutionFunctionalitySagas saga =
                new GetOpenedTournamentsForExecutionFunctionalitySagas(
                        unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournaments();
    }

    public List<TournamentDto> getClosedTournamentsForExecution(Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("getClosedTournamentsForExecution");
        GetClosedTournamentsForExecutionFunctionalitySagas saga =
                new GetClosedTournamentsForExecutionFunctionalitySagas(
                        unitOfWorkService, executionAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournaments();
    }

    public TournamentDto createTournament(Integer executionAggregateId, Integer creatorAggregateId,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          Integer numberOfQuestions, List<Integer> topicAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("createTournament");
        CreateTournamentFunctionalitySagas saga = new CreateTournamentFunctionalitySagas(
                unitOfWorkService, executionAggregateId, creatorAggregateId, startTime, endTime,
                numberOfQuestions, topicAggregateIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournamentDto();
    }

    public void addParticipant(Integer tournamentAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("addParticipant");
        AddParticipantFunctionalitySagas saga = new AddParticipantFunctionalitySagas(
                unitOfWorkService, tournamentAggregateId, userAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void updateTournament(Integer tournamentAggregateId, LocalDateTime startTime,
                                 LocalDateTime endTime, Integer numberOfQuestions,
                                 List<Integer> topicAggregateIds) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateTournament");
        UpdateTournamentFunctionalitySagas saga = new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, tournamentAggregateId, startTime, endTime, numberOfQuestions,
                topicAggregateIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void cancelTournament(Integer tournamentAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("cancelTournament");
        CancelTournamentFunctionalitySagas saga = new CancelTournamentFunctionalitySagas(
                unitOfWorkService, tournamentAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteTournament(Integer tournamentAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("deleteTournament");
        DeleteTournamentFunctionalitySagas saga = new DeleteTournamentFunctionalitySagas(
                unitOfWorkService, tournamentAggregateId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void setUserNameByEvent(Integer tournamentAggregateId, Integer userAggregateId, String userName,
                                   Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setUserNameByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.setUserName(tournamentAggregateId, userAggregateId, userName, userVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void anonymizeUserByEvent(Integer tournamentAggregateId, Integer userAggregateId, String userName,
                                     String userUsername, Long userVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeUserByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.anonymizeUser(tournamentAggregateId, userAggregateId, userName, userUsername,
                userVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDeletedUserByEvent(Integer tournamentAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDeletedUserByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.removeForDeletedUser(tournamentAggregateId, userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void setTopicNameByEvent(Integer tournamentAggregateId, Integer topicAggregateId, String topicName,
                                    Long topicVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("setTopicNameByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.setTopicName(tournamentAggregateId, topicAggregateId, topicName, topicVersion,
                unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeDeletedTopicByEvent(Integer tournamentAggregateId, Integer topicAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeDeletedTopicByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.removeDeletedTopic(tournamentAggregateId, topicAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForDeletedExecutionByEvent(Integer tournamentAggregateId, Integer executionAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForDeletedExecutionByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.removeForDeletedExecution(tournamentAggregateId, executionAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeDisenrolledParticipantByEvent(Integer tournamentAggregateId,
                                                    Integer executionAggregateId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeDisenrolledParticipantByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.removeDisenrolledParticipant(tournamentAggregateId, executionAggregateId,
                userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeForInvalidatedQuizByEvent(Integer tournamentAggregateId, Integer quizAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeForInvalidatedQuizByEvent");
        if (isInSaga(tournamentAggregateId, unitOfWork)) {
            return;
        }
        tournamentService.removeForInvalidatedQuiz(tournamentAggregateId, quizAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // No saga guard: the participant's answer statistics are the one cached group no saga step of this
    // aggregate writes, so there is no in-flight value for the event to overwrite.
    public void recordQuestionAnswerByEvent(Integer tournamentAggregateId, Integer quizAggregateId,
                                            Integer studentAggregateId, Integer quizAnswerAggregateId,
                                            Boolean correct, LocalDateTime answerTime,
                                            Long quizAnswerVersion) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("recordQuestionAnswerByEvent");
        tournamentService.recordQuestionAnswer(tournamentAggregateId, quizAggregateId, studentAggregateId,
                quizAnswerAggregateId, correct, answerTime, quizAnswerVersion, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    // The guard belongs here rather than in the service methods, which the saga steps also call.
    private boolean isInSaga(Integer tournamentAggregateId, SagaUnitOfWork unitOfWork) {
        SagaAggregate tournament = (SagaAggregate) unitOfWorkService.aggregateLoadAndRegisterRead(
                tournamentAggregateId, unitOfWork);
        return !GenericSagaState.NOT_IN_SAGA.equals(tournament.getSagaState());
    }
}
