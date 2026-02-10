package com.generated.abstractions.coordination.functionalities;

import static com.generated.ms.TransactionalModel.SAGAS;
import static com.generated.abstractions.microservices.exception.AbstractionsErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.abstractions.microservices.exception.AbstractionsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.abstractions.sagas.coordination.customer.*;
import pt.ulisboa.tecnico.socialsoftware.abstractions.microservices.customer.service.CustomerService;
import pt.ulisboa.tecnico.socialsoftware.abstractions.shared.dtos.CustomerDto;
import pt.ulisboa.tecnico.socialsoftware.abstractions.coordination.webapi.requestDtos.CreateCustomerRequestDto;
import java.util.List;

@Service
public class CustomerFunctionalities {
    @Autowired
    private CustomerService customerService;

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
            throw new AbstractionsException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CustomerDto createCustomer(CreateCustomerRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreateCustomerFunctionalitySagas createCustomerFunctionalitySagas = new CreateCustomerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, customerService, createRequest);
                createCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createCustomerFunctionalitySagas.getCreatedCustomerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CustomerDto getCustomerById(Integer customerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetCustomerByIdFunctionalitySagas getCustomerByIdFunctionalitySagas = new GetCustomerByIdFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, customerService, customerAggregateId);
                getCustomerByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getCustomerByIdFunctionalitySagas.getCustomerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public CustomerDto updateCustomer(CustomerDto customerDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(customerDto);
                UpdateCustomerFunctionalitySagas updateCustomerFunctionalitySagas = new UpdateCustomerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, customerService, customerDto);
                updateCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updateCustomerFunctionalitySagas.getUpdatedCustomerDto();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deleteCustomer(Integer customerAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeleteCustomerFunctionalitySagas deleteCustomerFunctionalitySagas = new DeleteCustomerFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, customerService, customerAggregateId);
                deleteCustomerFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<CustomerDto> getAllCustomers() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllCustomersFunctionalitySagas getAllCustomersFunctionalitySagas = new GetAllCustomersFunctionalitySagas(
                        sagaUnitOfWork, sagaUnitOfWorkService, customerService);
                getAllCustomersFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllCustomersFunctionalitySagas.getCustomers();
            default: throw new AnswersException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(CustomerDto customerDto) {
        if (customerDto.getName() == null) {
            throw new AbstractionsException(CUSTOMER_MISSING_NAME);
        }
        if (customerDto.getEmail() == null) {
            throw new AbstractionsException(CUSTOMER_MISSING_EMAIL);
        }
}

    private void checkInput(CreateCustomerRequestDto createRequest) {
        if (createRequest.getName() == null) {
            throw new AbstractionsException(CUSTOMER_MISSING_NAME);
        }
        if (createRequest.getEmail() == null) {
            throw new AbstractionsException(CUSTOMER_MISSING_EMAIL);
        }
}
}