package pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.answers.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.answers.command.tournament.*;
import pt.ulisboa.tecnico.socialsoftware.answers.shared.dtos.TournamentDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.answers.microservices.tournament.coordination.webapi.requestDtos.CreateTournamentRequestDto;

public class CreateTournamentFunctionalitySagas extends WorkflowFunctionality {
    private TournamentDto createdTournamentDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateTournamentFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateTournamentRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateTournamentRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createTournamentStep = new SagaStep("createTournamentStep", () -> {
            CreateTournamentCommand cmd = new CreateTournamentCommand(unitOfWork, ServiceMapping.TOURNAMENT.getServiceName(), createRequest);
            TournamentDto createdTournamentDto = (TournamentDto) commandGateway.send(cmd);
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
