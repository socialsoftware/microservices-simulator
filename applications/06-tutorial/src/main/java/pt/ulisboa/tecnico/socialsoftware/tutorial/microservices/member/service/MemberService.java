package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.aggregate.*;

import java.util.List;
import java.util.Set;
import java.util.Optional;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;

import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.enums.MembershipType;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.domain.aggregate.AggregateIdGeneratorService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberDeletedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.MemberUpdatedEvent;
import pt.ulisboa.tecnico.socialsoftware.tutorial.events.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi.requestDtos.CreateMemberRequestDto;


@Service
@Transactional(noRollbackFor = TutorialException.class)
public class MemberService {
    @Autowired
    private AggregateIdGeneratorService aggregateIdGeneratorService;

    @Autowired
    private UnitOfWorkService unitOfWorkService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private MemberFactory memberFactory;

    @Autowired
    private MemberServiceExtension extension;

    public MemberService() {}

    public MemberDto createMember(CreateMemberRequestDto createRequest, UnitOfWork unitOfWork) {
        try {
            MemberDto memberDto = new MemberDto();
            memberDto.setName(createRequest.getName());
            memberDto.setEmail(createRequest.getEmail());
            memberDto.setMembership(createRequest.getMembership() != null ? createRequest.getMembership().name() : null);

            Integer aggregateId = aggregateIdGeneratorService.getNewAggregateId();
            Member member = memberFactory.createMember(aggregateId, memberDto);
            unitOfWorkService.registerChanged(member, unitOfWork);
            return memberFactory.createMemberDto(member);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error creating member: " + e.getMessage());
        }
    }

    public MemberDto getMemberById(Integer id, UnitOfWork unitOfWork) {
        try {
            Member member = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            return memberFactory.createMemberDto(member);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving member: " + e.getMessage());
        }
    }

    public List<MemberDto> getAllMembers(UnitOfWork unitOfWork) {
        try {
            Set<Integer> aggregateIds = memberRepository.findAll().stream()
                .map(Member::getAggregateId)
                .collect(Collectors.toSet());

            return aggregateIds.stream()
                .map(id -> {
                    try {
                        return (Member) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(java.util.Objects::nonNull)
                .map(memberFactory::createMemberDto)
                .collect(Collectors.toList());
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error retrieving member: " + e.getMessage());
        }
    }

    public MemberDto updateMember(MemberDto memberDto, UnitOfWork unitOfWork) {
        try {
            Integer id = memberDto.getAggregateId();
            Member oldMember = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Member newMember = memberFactory.createMemberFromExisting(oldMember);
            if (memberDto.getName() != null) {
                newMember.setName(memberDto.getName());
            }
            if (memberDto.getEmail() != null) {
                newMember.setEmail(memberDto.getEmail());
            }
            if (memberDto.getMembership() != null) {
                newMember.setMembership(MembershipType.valueOf(memberDto.getMembership()));
            }

            unitOfWorkService.registerChanged(newMember, unitOfWork);            MemberUpdatedEvent event = new MemberUpdatedEvent(newMember.getAggregateId(), newMember.getName(), newMember.getEmail());
            event.setPublisherAggregateVersion(newMember.getVersion());
            unitOfWorkService.registerEvent(event, unitOfWork);
            return memberFactory.createMemberDto(newMember);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error updating member: " + e.getMessage());
        }
    }

    public void deleteMember(Integer id, UnitOfWork unitOfWork) {
        try {
            Member oldMember = (Member) unitOfWorkService.aggregateLoadAndRegisterRead(id, unitOfWork);
            Member newMember = memberFactory.createMemberFromExisting(oldMember);
            newMember.remove();
            unitOfWorkService.registerChanged(newMember, unitOfWork);            unitOfWorkService.registerEvent(new MemberDeletedEvent(newMember.getAggregateId()), unitOfWork);
        } catch (TutorialException e) {
            throw e;
        } catch (Exception e) {
            throw new TutorialException("Error deleting member: " + e.getMessage());
        }
    }








}