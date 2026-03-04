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

public class UpdateMemberFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto updatedMemberDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public UpdateMemberFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, MemberDto memberDto, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(memberDto, unitOfWork);
    }

    public void buildWorkflow(MemberDto memberDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep updateMemberStep = new SagaStep("updateMemberStep", () -> {
            UpdateMemberCommand cmd = new UpdateMemberCommand(unitOfWork, ServiceMapping.MEMBER.getServiceName(), memberDto);
            MemberDto updatedMemberDto = (MemberDto) commandGateway.send(cmd);
            setUpdatedMemberDto(updatedMemberDto);
        });

        workflow.addStep(updateMemberStep);
    }
    public MemberDto getUpdatedMemberDto() {
        return updatedMemberDto;
    }

    public void setUpdatedMemberDto(MemberDto updatedMemberDto) {
        this.updatedMemberDto = updatedMemberDto;
    }
}
