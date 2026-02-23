package pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.factories;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.Member;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.MemberFactory;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.SagaMember;
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.aggregates.dtos.SagaMemberDto;

@Service
@Profile("sagas")
public class SagasMemberFactory implements MemberFactory {
    @Override
    public Member createMember(Integer aggregateId, MemberDto memberDto) {
        return new SagaMember(aggregateId, memberDto);
    }

    @Override
    public Member createMemberFromExisting(Member existingMember) {
        return new SagaMember((SagaMember) existingMember);
    }

    @Override
    public MemberDto createMemberDto(Member member) {
        return new SagaMemberDto(member);
    }
}