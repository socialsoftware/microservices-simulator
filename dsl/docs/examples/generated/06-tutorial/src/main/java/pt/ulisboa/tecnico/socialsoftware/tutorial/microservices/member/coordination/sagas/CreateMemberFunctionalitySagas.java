package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.member.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi.requestDtos.CreateMemberRequestDto;

public class CreateMemberFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto createdMemberDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public CreateMemberFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, CreateMemberRequestDto createRequest, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateMemberRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep createMemberStep = new SagaStep("createMemberStep", () -> {
            CreateMemberCommand cmd = new CreateMemberCommand(unitOfWork, ServiceMapping.MEMBER.getServiceName(), createRequest);
            MemberDto createdMemberDto = (MemberDto) commandGateway.send(cmd);
            setCreatedMemberDto(createdMemberDto);
        });

        workflow.addStep(createMemberStep);
    }
    public MemberDto getCreatedMemberDto() {
        return createdMemberDto;
    }

    public void setCreatedMemberDto(MemberDto createdMemberDto) {
        this.createdMemberDto = createdMemberDto;
    }
}
