package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.commandHandler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.Command;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandHandler;
import pt.ulisboa.tecnico.socialsoftware.tutorial.command.member.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;

import java.util.logging.Logger;

@Component
public class MemberCommandHandler extends CommandHandler {
    private static final Logger logger = Logger.getLogger(MemberCommandHandler.class.getName());

    @Autowired
    private MemberService memberService;

    @Override
    protected String getAggregateTypeName() {
        return "Member";
    }

    @Override
    protected Object handleDomainCommand(Command command) {
        return switch (command) {
            case CreateMemberCommand cmd -> handleCreateMember(cmd);
            case GetMemberByIdCommand cmd -> handleGetMemberById(cmd);
            case GetAllMembersCommand cmd -> handleGetAllMembers(cmd);
            case UpdateMemberCommand cmd -> handleUpdateMember(cmd);
            case DeleteMemberCommand cmd -> handleDeleteMember(cmd);
            default -> {
                logger.warning("Unknown command type: " + command.getClass().getName());
                yield null;
            }
        };
    }

    private Object handleCreateMember(CreateMemberCommand cmd) {
        logger.info("handleCreateMember");
        try {
            return memberService.createMember(cmd.getCreateRequest(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetMemberById(GetMemberByIdCommand cmd) {
        logger.info("handleGetMemberById");
        try {
            return memberService.getMemberById(cmd.getRootAggregateId(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleGetAllMembers(GetAllMembersCommand cmd) {
        logger.info("handleGetAllMembers");
        try {
            return memberService.getAllMembers(cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleUpdateMember(UpdateMemberCommand cmd) {
        logger.info("handleUpdateMember");
        try {
            return memberService.updateMember(cmd.getMemberDto(), cmd.getUnitOfWork());
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }

    private Object handleDeleteMember(DeleteMemberCommand cmd) {
        logger.info("handleDeleteMember");
        try {
            memberService.deleteMember(cmd.getRootAggregateId(), cmd.getUnitOfWork());
            return null;
        } catch (Exception e) {
            logger.severe("Failed: " + e.getMessage());
            return e;
        }
    }
}
