package pt.ulisboa.tecnico.socialsoftware.answers.sagas.coordination.tournament;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.service.TournamentService;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.coordination.webapi.requestDtos.CreateTournamentRequestDto;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto createdTournamentDto;
    private final TournamentService tournamentService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateTournamentFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, TournamentService tournamentService, CreateTournamentRequestDto createRequest) {
        this.tournamentService = tournamentService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTournamentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createTournamentStep = new SagaSyncStep("createTournamentStep", () -> {
            TournamentDto createdTournamentDto = tournamentService.createTournament(createRequest, unitOfWork);
            setCreatedTournamentDto(createdTournamentDto);
        });

        workflow.addStep(createTournamentStep);

    }

    public TournamentDto getCreatedTournamentDto() {
        return createdTournamentDto;
    }

    public void setCreatedTournamentDto(TournamentDto createdTournamentDto) {
        this.createdTournamentDto = createdTournamentDto;
    }
}
