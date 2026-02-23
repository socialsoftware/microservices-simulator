package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.WorkflowFunctionality;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaSyncStep;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.workflow.SagaWorkflow;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateMemberRequestDto;

public class CreateMemberFunctionalitySagas extends WorkflowFunctionality {
    private MemberDto createdMemberDto;
    private final MemberService memberService;
    private final SagaUnitOfWorkService unitOfWorkService;


    public CreateMemberFunctionalitySagas(SagaUnitOfWork unitOfWork, SagaUnitOfWorkService unitOfWorkService, MemberService memberService, CreateMemberRequestDto createRequest) {
        this.memberService = memberService;
        this.unitOfWorkService = unitOfWorkService;
        this.buildWorkflow(createRequest, unitOfWork);
    }

    public void buildWorkflow(CreateMemberRequestDto createRequest, SagaUnitOfWork unitOfWork) {
        this.workflow = new SagaWorkflow(this, unitOfWorkService, unitOfWork);

        SagaSyncStep createMemberStep = new SagaSyncStep("createMemberStep", () -> {
            MemberDto createdMemberDto = memberService.createMember(createRequest, unitOfWork);
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
