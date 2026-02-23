package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;

public class UpdateMemberFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto updatedMemberDto;
    private final MemberService memberService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public UpdateMemberFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, MemberService memberService, MemberDto memberDto) {
        this.memberService = memberService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(memberDto, unitOfWork);
    }

    public void buildWorkflow(MemberDto memberDto, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep updateMemberStep = new SagaSyncStep("updateMemberStep", () -> {
            MemberDto updatedMemberDto = memberService.updateMember(memberDto, unitOfWork);
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
