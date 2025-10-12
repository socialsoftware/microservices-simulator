package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.aggregate.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.answers.sagas.aggregates.dtos.SagaTournamentDto;

@Component
public class TournamentSagaFunctionality extends WorkflowFunctionality {
private final TournamentService tournamentService;
private final SagaUnitOfWorkService unitOfWorkService;

public TournamentSagaFunctionality(TournamentService tournamentService, SagaUnitOfWorkService
unitOfWorkService) {
this.tournamentService = tournamentService;
this.unitOfWorkService = unitOfWorkService;
}


}