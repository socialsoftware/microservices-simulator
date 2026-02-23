package pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.tutorial.sagas.coordination.loan.*;
import pt.ulisboa.tecnico.socialsoftware.tutorial.microservices.loan.service.LoanService;
import pt.ulisboa.tecnico.socialsoftware.tutorial.shared.dtos.LoanDto;
import pt.ulisboa.tecnico.socialsoftware.tutorial.coordination.webapi.requestDtos.CreateLoanRequestDto;
import java.util.List;

@Service
public class LoanFunctionalities {
    @Autowired
    private LoanService loanService;

    @Autowired
    private SagaUnitOfWorkService sagaUnitOfWorkService;


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
                        sagaUnitOfWork, sagaUnitOfWorkService, loanService, createRequest);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, loanService, loanAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, loanService, loanDto);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, loanService, loanAggregateId);
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
                        sagaUnitOfWork, sagaUnitOfWorkService, loanService);
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