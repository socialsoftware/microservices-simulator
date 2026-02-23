package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.eventProcessing;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.unitOfWork.UnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;

@Service
public class MemberEventProcessing {
    @Autowired
    private MemberService memberService;
    
    private final UnitOfWorkService<UnitOfWork> unitOfWorkService;

    public MemberEventProcessing(UnitOfWorkService unitOfWorkService) {
        this.unitOfWorkService = unitOfWorkService;
    }}