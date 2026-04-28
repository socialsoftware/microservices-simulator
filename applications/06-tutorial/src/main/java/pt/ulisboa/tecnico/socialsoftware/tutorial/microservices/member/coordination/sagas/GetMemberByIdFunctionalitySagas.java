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

public class GetMemberByIdFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto memberDto;
    private final SagaUnitOfWorkService unitOfWorkService;
    private final CommandGateway commandGateway;


    public GetMemberByIdFunctionalitySagas(SagaUnitOfWorkService unitOfWorkService, Integer memberAggregateId, SagaUnitOfWork unitOfWork, CommandGateway commandGateway) {
        this.unitOfWorkService = unitOfWorkService;
        this.commandGateway = commandGateway;
        this.buildWorkflow(memberAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer memberAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaStep getMemberStep = new SagaStep("getMemberStep", () -> {
            unitOfWorkService.verifySagaState(memberAggregateId, new java.util.ArrayList<SagaState>(java.util.Arrays.asList(MemberSagaState.UPDATE_MEMBER, MemberSagaState.DELETE_MEMBER)));
            unitOfWorkService.registerSagaState(memberAggregateId, MemberSagaState.READ_MEMBER, unitOfWork);
            GetMemberByIdCommand cmd = new GetMemberByIdCommand(unitOfWork, ServiceMapping.MEMBER.getServiceName(), memberAggregateId);
            MemberDto memberDto = (MemberDto) commandGateway.send(cmd);
            setMemberDto(memberDto);
        });

        workflow.addStep(getMemberStep);
    }
    public MemberDto getMemberDto() {
        return memberDto;
    }

    public void setMemberDto(MemberDto memberDto) {
        this.memberDto = memberDto;
    }
}
