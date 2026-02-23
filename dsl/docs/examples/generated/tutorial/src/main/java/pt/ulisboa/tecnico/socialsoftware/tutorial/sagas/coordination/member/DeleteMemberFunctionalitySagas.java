package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class DeleteMemberFunctionalitySagas extends WorkflowFunctionality {
    private final MemberService memberService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public DeleteMemberFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, MemberService memberService, Integer memberAggregateId) {
        this.memberService = memberService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(memberAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer memberAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep deleteMemberStep = new SagaSyncStep("deleteMemberStep", () -> {
            memberService.deleteMember(memberAggregateId, unitOfWork);
        });

        workflow.addStep(deleteMemberStep);
    }
}
