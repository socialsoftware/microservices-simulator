package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.exception.TutorialException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.service.MemberService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.MemberDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.member.coordination.webapi.requestDtos.CreateMemberRequestDto;
import java.util.List;

@Service
public class MemberFunctionalities {
    @Autowired
    private MemberService memberService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;

    @Autowired
    private CommandGateway commandGateway;


    @Autowired
    private Environment env;

    private TransactionalModel workflowType;

    @PostConstruct
    public void init() {
        String[] activeProfiles = env.getActiveProfiles();
        if (Arrays.asList(activeProfiles).contains(SAGAS.getValue())) {
            workflowType = SAGAS;
        } else {
            throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public MemberDto createMember(CreateMemberRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateMemberFunctionalitySagas createMemberFunctionalitySagas = new CreateMemberFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createMemberFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createMemberFunctionalitySagas.getCreatedMemberDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public MemberDto getMemberById(Integer memberAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetMemberByIdFunctionalitySagas getMemberByIdFunctionalitySagas = new GetMemberByIdFunctionalitySagas(
                        sagaUnitOfWorkService, memberAggregateId, sagaUnitOfWork, commandGateway);
                getMemberByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getMemberByIdFunctionalitySagas.getMemberDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public MemberDto updateMember(MemberDto memberDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(memberDto);
                UpdateMemberFunctionalitySagas updateMemberFunctionalitySagas = new UpdateMemberFunctionalitySagas(
                        sagaUnitOfWorkService, memberDto, sagaUnitOfWork, commandGateway);
                updateMemberFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateMemberFunctionalitySagas.getUpdatedMemberDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteMember(Integer memberAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteMemberFunctionalitySagas deleteMemberFunctionalitySagas = new DeleteMemberFunctionalitySagas(
                        sagaUnitOfWorkService, memberAggregateId, sagaUnitOfWork, commandGateway);
                deleteMemberFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<MemberDto> getAllMembers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllMembersFunctionalitySagas getAllMembersFunctionalitySagas = new GetAllMembersFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllMembersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllMembersFunctionalitySagas.getMembers();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(MemberDto memberDto) {
        if (memberDto.getName() == null) {
            throw new TutorialException(MEMBER_MISSING_NAME);
        }
        if (memberDto.getEmail() == null) {
            throw new TutorialException(MEMBER_MISSING_EMAIL);
        }
}

    private void checkInput(CreateMemberRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new TutorialException(MEMBER_MISSING_NAME);
        }
        if (createRequest.getEmail() == null) {
            throw new TutorialException(MEMBER_MISSING_EMAIL);
        }
}
}