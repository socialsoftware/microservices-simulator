package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import java.util.List;

public class GetAllMembersFunctionalitySagas extends WorkflowFunctionality {
    private List<MemberDto> members;
    private final MemberService memberService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public GetAllMembersFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, MemberService memberService) {
        this.memberService = memberService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(unitOfWork);
    }

    public void buildWorkflow(SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep getAllMembersStep = new SagaSyncStep("getAllMembersStep", () -> {
            List<MemberDto> members = memberService.getAllMembers(unitOfWork);
            setMembers(members);
        });

        workflow.addStep(getAllMembersStep);
    }
    public List<MemberDto> getMembers() {
        return members;
    }

    public void setMembers(List<MemberDto> members) {
        this.members = members;
    }
}
