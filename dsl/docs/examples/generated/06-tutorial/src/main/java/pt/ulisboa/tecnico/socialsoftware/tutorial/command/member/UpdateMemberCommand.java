package pt.ulisboa.tecnico.socialsoftware.tutorial.command.member;

import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.Command;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;

public class UpdateMemberCommand extends Command {
    private final MemberDto memberDto;

    public UpdateMemberCommand(UnitOfWork unitOfWork, String serviceName, MemberDto memberDto) {
        super(unitOfWork, serviceName, null);
        this.memberDto = memberDto;
    }

    public MemberDto getMemberDto() { return memberDto; }
}
