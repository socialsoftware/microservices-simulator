package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
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

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TournamentFunctionalities {
    @Autowired
    private SagaUnitOfWorkService unitOfWorkService;
    @Autowired
    private CommandGateway commandGateway;

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
}
