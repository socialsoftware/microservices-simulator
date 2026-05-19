package pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.AddParticipantFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.CancelTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.CreateTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.DeleteTournamentFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.GetOpenTournamentsFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.GetTournamentByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull.microservices.tournament.coordination.sagas.UpdateTournamentFunctionalitySagas;

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

    public TournamentDto getTournamentById(Integer tournamentId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetTournamentByIdFunctionalitySagas saga = new GetTournamentByIdFunctionalitySagas(
                unitOfWorkService, tournamentId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournamentDto();
    }

    public List<TournamentDto> getOpenTournaments(Integer executionId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        GetOpenTournamentsFunctionalitySagas saga = new GetOpenTournamentsFunctionalitySagas(
                unitOfWorkService, executionId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getTournaments();
    }

    public TournamentDto createTournament(Integer executionId, Integer creatorId,
                                           List<Integer> topicIds, Integer numberOfQuestions,
                                           LocalDateTime startTime, LocalDateTime endTime) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CreateTournamentFunctionalitySagas saga = new CreateTournamentFunctionalitySagas(
                unitOfWorkService, executionId, creatorId, topicIds, numberOfQuestions,
                startTime, endTime, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
        return saga.getCreatedTournamentDto();
    }

    public void addParticipant(Integer tournamentId, Integer executionId, Integer userId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        AddParticipantFunctionalitySagas saga = new AddParticipantFunctionalitySagas(
                unitOfWorkService, tournamentId, executionId, userId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void updateTournament(Integer tournamentId, LocalDateTime startTime, LocalDateTime endTime,
                                  List<Integer> topicIds) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        UpdateTournamentFunctionalitySagas saga = new UpdateTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, startTime, endTime, topicIds, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void cancelTournament(Integer tournamentId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        CancelTournamentFunctionalitySagas saga = new CancelTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void deleteTournament(Integer tournamentId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork(functionalityName);
        DeleteTournamentFunctionalitySagas saga = new DeleteTournamentFunctionalitySagas(
                unitOfWorkService, tournamentId, unitOfWork, commandGateway);
        saga.executeWorkflow(unitOfWork);
    }

    public void removeUserFromTournamentByEvent(Integer tournamentId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeUserFromTournamentByEvent");
        tournamentService.removeUserFromTournamentByEvent(tournamentId, userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void updateStudentNameByEvent(Integer tournamentId, Integer userAggregateId, String name) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateStudentNameByEvent");
        tournamentService.updateStudentNameByEvent(tournamentId, userAggregateId, name, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void anonymizeStudentByEvent(Integer tournamentId, Integer userAggregateId, String name, String username) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("anonymizeStudentByEvent");
        tournamentService.anonymizeStudentByEvent(tournamentId, userAggregateId, name, username, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void updateTopicNameByEvent(Integer tournamentId, Integer topicAggregateId, String topicName) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateTopicNameByEvent");
        tournamentService.updateTopicNameByEvent(tournamentId, topicAggregateId, topicName, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeTopicByEvent(Integer tournamentId, Integer topicAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeTopicByEvent");
        tournamentService.removeTopicByEvent(tournamentId, topicAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeTournamentByExecutionByEvent(Integer tournamentId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeTournamentByExecutionByEvent");
        tournamentService.removeTournamentByExecutionByEvent(tournamentId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void removeTournamentByQuizByEvent(Integer tournamentId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("removeTournamentByQuizByEvent");
        tournamentService.removeTournamentByQuizByEvent(tournamentId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }

    public void updateParticipantAnsweredByEvent(Integer tournamentId, Integer userAggregateId) {
        SagaUnitOfWork unitOfWork = unitOfWorkService.createUnitOfWork("updateParticipantAnsweredByEvent");
        tournamentService.updateParticipantAnsweredByEvent(tournamentId, userAggregateId, unitOfWork);
        unitOfWorkService.commit(unitOfWork);
    }
}
