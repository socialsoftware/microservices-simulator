package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;

import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.Aggregate;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.event.EventSubscription;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.enums.MembershipType;

@Entity
public abstract class Member extends Aggregate {
    private String name;
    private String email;
    @Enumerated(EnumType.STRING)
    private MembershipType membership;

    public Member() {

    }

    public Member(Integer aggregateId, MemberDto memberDto) {
        super(aggregateId);
        setAggregateType(getClass().getSimpleName());
        setName(memberDto.getName());
        setEmail(memberDto.getEmail());
        setMembership(MembershipType.valueOf(memberDto.getMembership()));
    }


    public Member(Member other) {
        super(other);
        setName(other.getName());
        setEmail(other.getEmail());
        setMembership(other.getMembership());
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public MembershipType getMembership() {
        return membership;
    }

    public void setMembership(MembershipType membership) {
        this.membership = membership;
    }


    @Override
    public Set<EventSubscription> getEventSubscriptions() {
        return new HashSet<>();
    }


    @Override
    public void verifyInvariants() {
    }

    public MemberDto buildDto() {
        MemberDto dto = new MemberDto();
        dto.setAggregateId(getAggregateId());
        dto.setVersion(getVersion());
        dto.setState(getState());
        dto.setName(getName());
        dto.setEmail(getEmail());
        dto.setMembership(getMembership() != null ? getMembership().name() : null);
        return dto;
    }
}