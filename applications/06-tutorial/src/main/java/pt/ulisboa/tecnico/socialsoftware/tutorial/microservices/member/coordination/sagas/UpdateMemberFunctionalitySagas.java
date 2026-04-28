package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.sagas;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.ServiceMapping;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.member.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.aggregate.SagaAggregate.SagaState;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.sagas.states.MemberSagaState;

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
            unitOfWorkService.verifySagaState(memberDto.getAggregateId(), new java.util.ArrayList<SagaState>(java.util.Arrays.asList(MemberSagaState.READ_MEMBER, MemberSagaState.UPDATE_MEMBER, MemberSagaState.DELETE_MEMBER)));
            unitOfWorkService.registerSagaState(memberDto.getAggregateId(), MemberSagaState.UPDATE_MEMBER, unitOfWork);
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
