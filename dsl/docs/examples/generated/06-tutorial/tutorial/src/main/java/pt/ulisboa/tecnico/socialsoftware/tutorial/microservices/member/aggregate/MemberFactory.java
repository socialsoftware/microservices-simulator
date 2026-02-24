package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;

public interface MemberFactory {
    Member createMember(Integer aggregateId, MemberDto memberDto);
    Member createMemberFromExisting(Member existingMember);
    MemberDto createMemberDto(Member member);
}
