package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class GetMemberByIdFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto memberDto;
    private final MemberService memberService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetMemberByIdFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, MemberService memberService, Integer memberAggregateId) {
        this.memberService = memberService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(memberAggregateId, unitOfWork);
    }

    public void buildWorkflow(Integer memberAggregateId, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getMemberStep = new SagaSyncStep("getMemberStep", () -> {
            MemberDto memberDto = memberService.getMemberById(memberAggregateId, unitOfWork);
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
