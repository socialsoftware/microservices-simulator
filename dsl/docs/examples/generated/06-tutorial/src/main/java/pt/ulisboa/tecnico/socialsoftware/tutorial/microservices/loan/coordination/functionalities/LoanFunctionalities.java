package pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.coordination.webapi.requestDtos.CreateLoanRequestDto;
import java.util.List;

@Service
public class LoanFunctionalities {
    @Autowired
    private LoanService loanService;

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

    public LoanDto createLoan(CreateLoanRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateLoanFunctionalitySagas createLoanFunctionalitySagas = new CreateLoanFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createLoanFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createLoanFunctionalitySagas.getCreatedLoanDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public LoanDto getLoanById(Integer loanAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetLoanByIdFunctionalitySagas getLoanByIdFunctionalitySagas = new GetLoanByIdFunctionalitySagas(
                        sagaUnitOfWorkService, loanAggregateId, sagaUnitOfWork, commandGateway);
                getLoanByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getLoanByIdFunctionalitySagas.getLoanDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public LoanDto updateLoan(LoanDto loanDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(loanDto);
                UpdateLoanFunctionalitySagas updateLoanFunctionalitySagas = new UpdateLoanFunctionalitySagas(
                        sagaUnitOfWorkService, loanDto, sagaUnitOfWork, commandGateway);
                updateLoanFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateLoanFunctionalitySagas.getUpdatedLoanDto();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteLoan(Integer loanAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteLoanFunctionalitySagas deleteLoanFunctionalitySagas = new DeleteLoanFunctionalitySagas(
                        sagaUnitOfWorkService, loanAggregateId, sagaUnitOfWork, commandGateway);
                deleteLoanFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<LoanDto> getAllLoans() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllLoansFunctionalitySagas getAllLoansFunctionalitySagas = new GetAllLoansFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllLoansFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllLoansFunctionalitySagas.getLoans();
            default: throw new TutorialException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(LoanDto loanDto) {
}

    private void checkInput(CreateLoanRequestDto createRequest) {
}
}