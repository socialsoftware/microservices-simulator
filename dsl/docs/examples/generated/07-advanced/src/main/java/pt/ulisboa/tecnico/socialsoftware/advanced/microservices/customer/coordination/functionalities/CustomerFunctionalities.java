package pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.functionalities;

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
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.advanced.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.advanced.microservices.customer.coordination.webapi.requestDtos.CreateCustomerRequestDto;
import java.util.List;

@Service
public class CustomerFunctionalities {
    @Autowired
    private CustomerService customerService;

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

    public CustomerDto createCustomer(CreateCustomerRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateCustomerFunctionalitySagas createCustomerFunctionalitySagas = new CreateCustomerFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCustomerFunctionalitySagas.getCreatedCustomerDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CustomerDto getCustomerById(Integer customerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCustomerByIdFunctionalitySagas getCustomerByIdFunctionalitySagas = new GetCustomerByIdFunctionalitySagas(
                        sagaUnitOfWorkService, customerAggregateId, sagaUnitOfWork, commandGateway);
                getCustomerByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCustomerByIdFunctionalitySagas.getCustomerDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CustomerDto updateCustomer(CustomerDto customerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(customerDto);
                UpdateCustomerFunctionalitySagas updateCustomerFunctionalitySagas = new UpdateCustomerFunctionalitySagas(
                        sagaUnitOfWorkService, customerDto, sagaUnitOfWork, commandGateway);
                updateCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCustomerFunctionalitySagas.getUpdatedCustomerDto();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteCustomer(Integer customerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteCustomerFunctionalitySagas deleteCustomerFunctionalitySagas = new DeleteCustomerFunctionalitySagas(
                        sagaUnitOfWorkService, customerAggregateId, sagaUnitOfWork, commandGateway);
                deleteCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CustomerDto> getAllCustomers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllCustomersFunctionalitySagas getAllCustomersFunctionalitySagas = new GetAllCustomersFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllCustomersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllCustomersFunctionalitySagas.getCustomers();
            default: throw new AdvancedException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CustomerDto customerDto) {
        if (customerDto.getName() == null) {
            throw new AdvancedException(CUSTOMER_MISSING_NAME);
        }
        if (customerDto.getEmail() == null) {
            throw new AdvancedException(CUSTOMER_MISSING_EMAIL);
        }
}

    private void checkInput(CreateCustomerRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new AdvancedException(CUSTOMER_MISSING_NAME);
        }
        if (createRequest.getEmail() == null) {
            throw new AdvancedException(CUSTOMER_MISSING_EMAIL);
        }
}
}