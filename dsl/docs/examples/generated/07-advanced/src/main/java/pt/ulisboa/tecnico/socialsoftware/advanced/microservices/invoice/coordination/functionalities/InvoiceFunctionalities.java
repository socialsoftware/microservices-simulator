package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.exception.AdvancedException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.service.InvoiceService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.InvoiceDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.invoice.coordination.webapi.requestDtos.CreateInvoiceRequestDto;
import java.util.List;

@Service
public class InvoiceFunctionalities {
    @Autowired
    private InvoiceService invoiceService;

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
            throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public InvoiceDto createInvoice(CreateInvoiceRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateInvoiceFunctionalitySagas createInvoiceFunctionalitySagas = new CreateInvoiceFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createInvoiceFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createInvoiceFunctionalitySagas.getCreatedInvoiceDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public InvoiceDto getInvoiceById(Integer invoiceAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetInvoiceByIdFunctionalitySagas getInvoiceByIdFunctionalitySagas = new GetInvoiceByIdFunctionalitySagas(
                        sagaUnitOfWorkService, invoiceAggregateId, sagaUnitOfWork, commandGateway);
                getInvoiceByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getInvoiceByIdFunctionalitySagas.getInvoiceDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public InvoiceDto updateInvoice(InvoiceDto invoiceDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(invoiceDto);
                UpdateInvoiceFunctionalitySagas updateInvoiceFunctionalitySagas = new UpdateInvoiceFunctionalitySagas(
                        sagaUnitOfWorkService, invoiceDto, sagaUnitOfWork, commandGateway);
                updateInvoiceFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateInvoiceFunctionalitySagas.getUpdatedInvoiceDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteInvoice(Integer invoiceAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteInvoiceFunctionalitySagas deleteInvoiceFunctionalitySagas = new DeleteInvoiceFunctionalitySagas(
                        sagaUnitOfWorkService, invoiceAggregateId, sagaUnitOfWork, commandGateway);
                deleteInvoiceFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<InvoiceDto> getAllInvoices() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllInvoicesFunctionalitySagas getAllInvoicesFunctionalitySagas = new GetAllInvoicesFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllInvoicesFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllInvoicesFunctionalitySagas.getInvoices();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(InvoiceDto invoiceDto) {
}

    private void checkInput(CreateInvoiceRequestDto createRequest) {
}
}