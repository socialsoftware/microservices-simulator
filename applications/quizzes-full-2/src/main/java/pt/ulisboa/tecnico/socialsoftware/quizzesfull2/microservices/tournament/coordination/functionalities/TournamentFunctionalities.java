package pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.functionalities;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.messaging.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.transaction.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetClosedTournamentsForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetOpenedTournamentsForExecutionFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetTournamentByIdFunctionalitySagas;
import pt.ulisboa.tecnico.socialsoftware.quizzesfull2.microservices.tournament.coordination.sagas.GetTournamentsForExecutionFunctionalitySagas;

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
}
