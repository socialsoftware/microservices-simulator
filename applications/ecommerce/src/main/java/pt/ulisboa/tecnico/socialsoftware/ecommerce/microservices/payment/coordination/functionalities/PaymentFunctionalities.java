package pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.functionalities;

import static pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel.SAGAS;
import static pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceErrorMessage.*;

import java.util.Arrays;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.exception.EcommerceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import pt.ulisboa.tecnico.socialsoftware.ms.TransactionalModel;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWork;
import pt.ulisboa.tecnico.socialsoftware.ms.sagas.unitOfWork.SagaUnitOfWorkService;
import pt.ulisboa.tecnico.socialsoftware.ms.coordination.workflow.command.CommandGateway;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.sagas.*;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.service.PaymentService;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.shared.dtos.PaymentDto;
import pt.ulisboa.tecnico.socialsoftware.ecommerce.microservices.payment.coordination.webapi.requestDtos.CreatePaymentRequestDto;
import java.util.List;

@Service
public class PaymentFunctionalities {
    @Autowired
    private PaymentService paymentService;

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
            throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PaymentDto createPayment(CreatePaymentRequestDto createRequest) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(createRequest);
                CreatePaymentFunctionalitySagas createPaymentFunctionalitySagas = new CreatePaymentFunctionalitySagas(
                        sagaUnitOfWorkService, createRequest, sagaUnitOfWork, commandGateway);
                createPaymentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return createPaymentFunctionalitySagas.getCreatedPaymentDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PaymentDto getPaymentById(Integer paymentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetPaymentByIdFunctionalitySagas getPaymentByIdFunctionalitySagas = new GetPaymentByIdFunctionalitySagas(
                        sagaUnitOfWorkService, paymentAggregateId, sagaUnitOfWork, commandGateway);
                getPaymentByIdFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getPaymentByIdFunctionalitySagas.getPaymentDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public PaymentDto updatePayment(PaymentDto paymentDto) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                checkInput(paymentDto);
                UpdatePaymentFunctionalitySagas updatePaymentFunctionalitySagas = new UpdatePaymentFunctionalitySagas(
                        sagaUnitOfWorkService, paymentDto, sagaUnitOfWork, commandGateway);
                updatePaymentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return updatePaymentFunctionalitySagas.getUpdatedPaymentDto();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public void deletePayment(Integer paymentAggregateId) {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                DeletePaymentFunctionalitySagas deletePaymentFunctionalitySagas = new DeletePaymentFunctionalitySagas(
                        sagaUnitOfWorkService, paymentAggregateId, sagaUnitOfWork, commandGateway);
                deletePaymentFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                break;
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    public List<PaymentDto> getAllPayments() {
        String functionalityName = new Throwable().getStackTrace()[0].getMethodName();

        switch (workflowType) {
            case SAGAS:
                SagaUnitOfWork sagaUnitOfWork = sagaUnitOfWorkService.createUnitOfWork(functionalityName);
                GetAllPaymentsFunctionalitySagas getAllPaymentsFunctionalitySagas = new GetAllPaymentsFunctionalitySagas(
                        sagaUnitOfWorkService, sagaUnitOfWork, commandGateway);
                getAllPaymentsFunctionalitySagas.executeWorkflow(sagaUnitOfWork);
                return getAllPaymentsFunctionalitySagas.getPayments();
            default: throw new EcommerceException(UNDEFINED_TRANSACTIONAL_MODEL);
        }
    }

    private void checkInput(PaymentDto paymentDto) {
        if (paymentDto.getAuthorizationCode() == null) {
            throw new EcommerceException(PAYMENT_MISSING_AUTHORIZATIONCODE);
        }
        if (paymentDto.getPaymentMethod() == null) {
            throw new EcommerceException(PAYMENT_MISSING_PAYMENTMETHOD);
        }
}

    private void checkInput(CreatePaymentRequestDto createRequest) {
        if (createRequest.getAuthorizationCode() == null) {
            throw new EcommerceException(PAYMENT_MISSING_AUTHORIZATIONCODE);
        }
        if (createRequest.getPaymentMethod() == null) {
            throw new EcommerceException(PAYMENT_MISSING_PAYMENTMETHOD);
        }
}
}